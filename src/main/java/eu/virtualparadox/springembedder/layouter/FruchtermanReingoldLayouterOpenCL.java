package eu.virtualparadox.springembedder.layouter;

import eu.virtualparadox.springembedder.EdgeWeightNormalizer;
import eu.virtualparadox.springembedder.ResourceLoader;
import eu.virtualparadox.springembedder.TimeWatch;
import eu.virtualparadox.springembedder.Vector2D;
import eu.virtualparadox.springembedder.renderercallback.AbstractRendererCallback;
import eu.virtualparadox.springembedder.renderercallback.NoOpRendererCallback;
import org.jgrapht.Graph;
import org.jocl.*;

import java.io.IOException;
import java.util.*;

import static org.jocl.CL.*;

public class FruchtermanReingoldLayouterOpenCL<V, E> extends AbstractLayouter<V, E> {

    private static final float C = 0.01f;
    private cl_context context;
    private cl_command_queue commandQueue;
    private cl_program program;
    private cl_kernel kernelRepulsive;
    private cl_kernel kernelAttractive;
    private cl_kernel kernelSummarize;
    private cl_kernel kernelUpdate;
    private cl_kernel kernelZeroArray;

    public FruchtermanReingoldLayouterOpenCL(final int width, final int height, final AbstractRendererCallback<V, E> callback) {
        super(width, height, callback);
        initCL();
    }

    public FruchtermanReingoldLayouterOpenCL(final int width, final int height) {
        super(width, height, new NoOpRendererCallback<>());
        initCL();
    }

    private void initCL() {
        CL.setExceptionsEnabled(true);

        int[] numPlatformsArray = new int[1];
        cl_platform_id[] platforms = new cl_platform_id[1];
        clGetPlatformIDs(platforms.length, platforms, numPlatformsArray);
        cl_platform_id platform = platforms[0];

        cl_device_id[] devices = new cl_device_id[1];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, devices.length, devices, null);
        cl_device_id device = devices[0];

        context = clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
        commandQueue = clCreateCommandQueue(context, device, 0, null);

        try {
            String source = ResourceLoader.loadResourceAsString("/fruchterman-reingold.cl");
            program = clCreateProgramWithSource(context, 1, new String[]{source}, null, null);
            clBuildProgram(program, 0, null, null, null, null);

            kernelRepulsive = clCreateKernel(program, "calculateRepulsiveForces", null);
            kernelAttractive = clCreateKernel(program, "calculateAttractiveForces", null);
            kernelSummarize = clCreateKernel(program, "summarizeForces", null);
            kernelUpdate = clCreateKernel(program, "updatePositions", null);
            kernelZeroArray = clCreateKernel(program, "zeroArray", null);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load OpenCL kernel", e);
        }
    }

    @Override
    public Map<V, Vector2D> layout(final Graph<V, E> graph, final int iterations) {
        int numVertices = graph.vertexSet().size();
        int numEdges = graph.edgeSet().size();

        float[] positions = new float[2 * numVertices];
        float[] repulsiveMatrix = new float[2 * numVertices * numVertices];
        float[] attractiveMatrix = new float[2 * numEdges * numVertices];
        float[] displacements = new float[2 * numVertices];
        int[] edges = new int[2 * numEdges];
        float[] weights = new float[numEdges];

        Map<V, Integer> vertexIndexMap = new HashMap<>();
        int idx = 0;
        for (V v : graph.vertexSet()) {
            vertexIndexMap.put(v, idx);
            positions[2 * idx] = random.nextInt(width);
            positions[2 * idx + 1] = random.nextInt(height);
            idx++;
        }

        idx = 0;
        final Map<E, Double> normalizedWeights = edgeWeightNormalizer.normalizeEdgeWeights(graph);
        for (E e : graph.edgeSet()) {
            V from = graph.getEdgeSource(e);
            V to = graph.getEdgeTarget(e);
            edges[2 * idx] = vertexIndexMap.get(from);
            edges[2 * idx + 1] = vertexIndexMap.get(to);
            weights[idx] = normalizedWeights.get(e).floatValue();
            idx++;
        }

        cl_mem positionsMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * 2 * numVertices, Pointer.to(positions), null);
        cl_mem repulsiveMatrixMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * 2 * numVertices * numVertices, Pointer.to(repulsiveMatrix), null);
        cl_mem attractiveMatrixMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * 2 * numEdges * numVertices, Pointer.to(attractiveMatrix), null);
        cl_mem displacementsMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * 2 * numVertices, Pointer.to(displacements), null);
        cl_mem edgesMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * 2 * numEdges, Pointer.to(edges), null);
        cl_mem weightsMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * numEdges, Pointer.to(weights), null);

        double optimalDistance = Math.sqrt((width * height) / numVertices) / 2;
        float temperature = 50f;

        final TimeWatch tw = TimeWatch.start();
        for (int i = 0; i < iterations; i++) {
            tw.reset();

            // Zero out repulsive and attractive matrices
            clSetKernelArg(kernelZeroArray, 0, Sizeof.cl_mem, Pointer.to(repulsiveMatrixMem));
            clSetKernelArg(kernelZeroArray, 1, Sizeof.cl_int, Pointer.to(new int[]{2 * numVertices * numVertices}));
            clEnqueueNDRangeKernel(commandQueue, kernelZeroArray, 1, null, new long[]{2 * numVertices * numVertices}, null, 0, null, null);

            clSetKernelArg(kernelZeroArray, 0, Sizeof.cl_mem, Pointer.to(attractiveMatrixMem));
            clSetKernelArg(kernelZeroArray, 1, Sizeof.cl_int, Pointer.to(new int[]{2 * numEdges * numVertices}));
            clEnqueueNDRangeKernel(commandQueue, kernelZeroArray, 1, null, new long[]{2 * numEdges * numVertices}, null, 0, null, null);

            // Calculate repulsive forces
            clSetKernelArg(kernelRepulsive, 0, Sizeof.cl_mem, Pointer.to(positionsMem));
            clSetKernelArg(kernelRepulsive, 1, Sizeof.cl_mem, Pointer.to(repulsiveMatrixMem));
            clSetKernelArg(kernelRepulsive, 2, Sizeof.cl_int, Pointer.to(new int[]{numVertices}));
            clSetKernelArg(kernelRepulsive, 3, Sizeof.cl_float, Pointer.to(new float[]{(float) optimalDistance}));
            clSetKernelArg(kernelRepulsive, 4, Sizeof.cl_float, Pointer.to(new float[]{C}));
            clEnqueueNDRangeKernel(commandQueue, kernelRepulsive, 1, null, new long[]{numVertices}, null, 0, null, null);

            // Calculate attractive forces
            clSetKernelArg(kernelAttractive, 0, Sizeof.cl_mem, Pointer.to(positionsMem));
            clSetKernelArg(kernelAttractive, 1, Sizeof.cl_mem, Pointer.to(attractiveMatrixMem));
            clSetKernelArg(kernelAttractive, 2, Sizeof.cl_mem, Pointer.to(edgesMem));
            clSetKernelArg(kernelAttractive, 3, Sizeof.cl_mem, Pointer.to(weightsMem));
            clSetKernelArg(kernelAttractive, 4, Sizeof.cl_int, Pointer.to(new int[]{numEdges}));
            clSetKernelArg(kernelAttractive, 5, Sizeof.cl_int, Pointer.to(new int[]{numVertices}));
            clSetKernelArg(kernelAttractive, 6, Sizeof.cl_float, Pointer.to(new float[]{(float) optimalDistance}));
            clSetKernelArg(kernelAttractive, 7, Sizeof.cl_float, Pointer.to(new float[]{C}));
            clEnqueueNDRangeKernel(commandQueue, kernelAttractive, 1, null, new long[]{numEdges}, null, 0, null, null);

            // Summarize forces
            clSetKernelArg(kernelSummarize, 0, Sizeof.cl_mem, Pointer.to(repulsiveMatrixMem));
            clSetKernelArg(kernelSummarize, 1, Sizeof.cl_mem, Pointer.to(attractiveMatrixMem));
            clSetKernelArg(kernelSummarize, 2, Sizeof.cl_mem, Pointer.to(displacementsMem));
            clSetKernelArg(kernelSummarize, 3, Sizeof.cl_int, Pointer.to(new int[]{numVertices}));
            clSetKernelArg(kernelSummarize, 4, Sizeof.cl_int, Pointer.to(new int[]{numEdges}));
            clEnqueueNDRangeKernel(commandQueue, kernelSummarize, 1, null, new long[]{numVertices}, null, 0, null, null);

            // Update positions
            clSetKernelArg(kernelUpdate, 0, Sizeof.cl_mem, Pointer.to(positionsMem));
            clSetKernelArg(kernelUpdate, 1, Sizeof.cl_mem, Pointer.to(displacementsMem));
            clSetKernelArg(kernelUpdate, 2, Sizeof.cl_int, Pointer.to(new int[]{numVertices}));
            clSetKernelArg(kernelUpdate, 3, Sizeof.cl_float, Pointer.to(new float[]{temperature}));
            clSetKernelArg(kernelUpdate, 4, Sizeof.cl_int, Pointer.to(new int[]{width}));
            clSetKernelArg(kernelUpdate, 5, Sizeof.cl_int, Pointer.to(new int[]{height}));
            clEnqueueNDRangeKernel(commandQueue, kernelUpdate, 1, null, new long[]{numVertices}, null, 0, null, null);

            // Call the callback before changing the temperature
            clEnqueueReadBuffer(commandQueue, positionsMem, CL_TRUE, 0, Sizeof.cl_float * 2 * numVertices, Pointer.to(positions), 0, null, null);
            final Map<V, Vector2D> positionsMap = new HashMap<>();
            for (Map.Entry<V, Integer> entry : vertexIndexMap.entrySet()) {
                V v = entry.getKey();
                idx = entry.getValue();
                positionsMap.put(v, new Vector2D(positions[2 * idx], positions[2 * idx + 1]));
            }
            logger.debug("Iteration {} took {}.", i, tw.toMilliSeconds());
            callback.render(graph, i, positionsMap);

            // Decrease temperature
            temperature = Math.max(1.5f, temperature * 0.95f);
        }
        callback.finish();

        // Read final positions from the device
        clEnqueueReadBuffer(commandQueue, positionsMem, CL_TRUE, 0, Sizeof.cl_float * 2 * numVertices, Pointer.to(positions), 0, null, null);

        Map<V, Vector2D> result = new HashMap<>();
        for (Map.Entry<V, Integer> entry : vertexIndexMap.entrySet()) {
            V v = entry.getKey();
            idx = entry.getValue();
            result.put(v, new Vector2D(positions[2 * idx], positions[2 * idx + 1]));
        }

        // Release OpenCL resources
        clReleaseMemObject(positionsMem);
        clReleaseMemObject(repulsiveMatrixMem);
        clReleaseMemObject(attractiveMatrixMem);
        clReleaseMemObject(displacementsMem);
        clReleaseMemObject(edgesMem);
        clReleaseMemObject(weightsMem);
        clReleaseKernel(kernelRepulsive);
        clReleaseKernel(kernelAttractive);
        clReleaseKernel(kernelUpdate);
        clReleaseKernel(kernelSummarize);
        clReleaseKernel(kernelZeroArray);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);

        return result;
    }
}
