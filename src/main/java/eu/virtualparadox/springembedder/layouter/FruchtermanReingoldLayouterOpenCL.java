package eu.virtualparadox.springembedder.layouter;

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

    private final cl_context context;
    private final cl_command_queue commandQueue;
    private final cl_program program;
    private final cl_kernel kernelRepulsive;
    private final cl_kernel kernelAttractive;
    private final cl_kernel kernelSummarize;
    private final cl_kernel kernelUpdate;

    public FruchtermanReingoldLayouterOpenCL(final int width, final int height, final AbstractRendererCallback<V, E> callback) {
        super(width, height, callback);
        this.context = initCLContext();
        this.commandQueue = initCLCommandQueue(this.context);
        this.program = initCLProgram(this.context);
        this.kernelRepulsive = initCLKernel(this.program, "calculateRepulsiveForces");
        this.kernelAttractive = initCLKernel(this.program, "calculateAttractiveForces");
        this.kernelSummarize = initCLKernel(this.program, "summarizeForces");
        this.kernelUpdate = initCLKernel(this.program, "updatePositions");
    }

    public FruchtermanReingoldLayouterOpenCL(final int width, final int height) {
        this(width, height, new NoOpRendererCallback<>());
    }

    private cl_context initCLContext() {
        CL.setExceptionsEnabled(true);

        final int[] numPlatformsArray = new int[1];
        final cl_platform_id[] platforms = new cl_platform_id[1];
        clGetPlatformIDs(platforms.length, platforms, numPlatformsArray);
        final cl_platform_id platform = platforms[0];

        final cl_device_id[] devices = new cl_device_id[1];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, devices.length, devices, null);
        final cl_device_id device = devices[0];

        return clCreateContext(null, 1, new cl_device_id[]{device}, null, null, null);
    }

    private cl_command_queue initCLCommandQueue(final cl_context context) {
        final cl_device_id[] devices = new cl_device_id[1];
        clGetContextInfo(context, CL_CONTEXT_DEVICES, Sizeof.cl_device_id * devices.length, Pointer.to(devices), null);
        final cl_device_id device = devices[0];

        return clCreateCommandQueue(context, device, 0, null);
    }

    private cl_program initCLProgram(final cl_context context) {
        try {
            final String source = ResourceLoader.loadResourceAsString("/fruchterman-reingold.cl");
            final cl_program program = clCreateProgramWithSource(context, 1, new String[]{source}, null, null);
            clBuildProgram(program, 0, null, null, null, null);

            return program;
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load OpenCL kernel", e);
        }
    }

    private cl_kernel initCLKernel(final cl_program program, final String kernelName) {
        return clCreateKernel(program, kernelName, null);
    }

    @Override
    public Map<V, Vector2D> layout(final Graph<V, E> graph, final int iterations) {
        final int numVertices = graph.vertexSet().size();
        final int numEdges = graph.edgeSet().size();

        final float[] positions = new float[2 * numVertices];
        final int[] repulsiveForces = new int[2 * numVertices];
        final int[] attractiveForces = new int[2 * numVertices];
        final float[] displacements = new float[2 * numVertices];
        final int[] edges = new int[2 * numEdges];
        final float[] weights = new float[numEdges];

        final Map<V, Integer> vertexIndexMap = new HashMap<>();
        int idx = 0;
        for (final V v : graph.vertexSet()) {
            vertexIndexMap.put(v, idx);
            positions[2 * idx] = random.nextInt(width);
            positions[2 * idx + 1] = random.nextInt(height);
            idx++;
        }

        idx = 0;
        final Map<E, Double> normalizedWeights = edgeWeightNormalizer.normalizeEdgeWeights(graph);
        for (final E e : graph.edgeSet()) {
            final V from = graph.getEdgeSource(e);
            final V to = graph.getEdgeTarget(e);
            edges[2 * idx] = vertexIndexMap.get(from);
            edges[2 * idx + 1] = vertexIndexMap.get(to);
            weights[idx] = normalizedWeights.get(e).floatValue();
            idx++;
        }

        final cl_mem positionsMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * 2 * numVertices, Pointer.to(positions), null);
        final cl_mem repulsiveForcesMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * 2 * numVertices, Pointer.to(repulsiveForces), null);
        final cl_mem attractiveForcesMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * 2 * numVertices, Pointer.to(attractiveForces), null);
        final cl_mem displacementsMem = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * 2 * numVertices, Pointer.to(displacements), null);
        final cl_mem edgesMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * 2 * numEdges, Pointer.to(edges), null);
        final cl_mem weightsMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * numEdges, Pointer.to(weights), null);

        final double optimalDistance = Math.sqrt((width * height) / numVertices) / 2;
        float temperature = 50f;

        final TimeWatch tw = TimeWatch.start();
        for (int i = 0; i < iterations; i++) {
            tw.reset();

            // Zero out repulsiveForces and attractiveForces
            Arrays.fill(repulsiveForces, 0);
            Arrays.fill(attractiveForces, 0);
            clEnqueueWriteBuffer(commandQueue, repulsiveForcesMem, CL_TRUE, 0, Sizeof.cl_int * 2 * numVertices, Pointer.to(repulsiveForces), 0, null, null);
            clEnqueueWriteBuffer(commandQueue, attractiveForcesMem, CL_TRUE, 0, Sizeof.cl_int * 2 * numVertices, Pointer.to(attractiveForces), 0, null, null);

            // Calculate repulsive forces
            clSetKernelArg(kernelRepulsive, 0, Sizeof.cl_mem, Pointer.to(positionsMem));
            clSetKernelArg(kernelRepulsive, 1, Sizeof.cl_mem, Pointer.to(repulsiveForcesMem));
            clSetKernelArg(kernelRepulsive, 2, Sizeof.cl_int, Pointer.to(new int[]{numVertices}));
            clSetKernelArg(kernelRepulsive, 3, Sizeof.cl_float, Pointer.to(new float[]{(float) optimalDistance}));
            clSetKernelArg(kernelRepulsive, 4, Sizeof.cl_float, Pointer.to(new float[]{C}));
            clEnqueueNDRangeKernel(commandQueue, kernelRepulsive, 1, null, new long[]{numVertices}, null, 0, null, null);

            // Calculate attractive forces
            clSetKernelArg(kernelAttractive, 0, Sizeof.cl_mem, Pointer.to(positionsMem));
            clSetKernelArg(kernelAttractive, 1, Sizeof.cl_mem, Pointer.to(attractiveForcesMem));
            clSetKernelArg(kernelAttractive, 2, Sizeof.cl_mem, Pointer.to(edgesMem));
            clSetKernelArg(kernelAttractive, 3, Sizeof.cl_mem, Pointer.to(weightsMem));
            clSetKernelArg(kernelAttractive, 4, Sizeof.cl_int, Pointer.to(new int[]{numEdges}));
            clSetKernelArg(kernelAttractive, 5, Sizeof.cl_int, Pointer.to(new int[]{numVertices}));
            clSetKernelArg(kernelAttractive, 6, Sizeof.cl_float, Pointer.to(new float[]{(float) optimalDistance}));
            clSetKernelArg(kernelAttractive, 7, Sizeof.cl_float, Pointer.to(new float[]{C}));
            clEnqueueNDRangeKernel(commandQueue, kernelAttractive, 1, null, new long[]{numEdges}, null, 0, null, null);

            // Summarize forces
            clSetKernelArg(kernelSummarize, 0, Sizeof.cl_mem, Pointer.to(repulsiveForcesMem));
            clSetKernelArg(kernelSummarize, 1, Sizeof.cl_mem, Pointer.to(attractiveForcesMem));
            clSetKernelArg(kernelSummarize, 2, Sizeof.cl_mem, Pointer.to(displacementsMem));
            clSetKernelArg(kernelSummarize, 3, Sizeof.cl_int, Pointer.to(new int[]{numVertices}));
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
            for (final Map.Entry<V, Integer> entry : vertexIndexMap.entrySet()) {
                final V v = entry.getKey();
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

        final Map<V, Vector2D> result = new HashMap<>();
        for (final Map.Entry<V, Integer> entry : vertexIndexMap.entrySet()) {
            final V v = entry.getKey();
            idx = entry.getValue();
            result.put(v, new Vector2D(positions[2 * idx], positions[2 * idx + 1]));
        }

        // Release OpenCL resources
        clReleaseMemObject(positionsMem);
        clReleaseMemObject(repulsiveForcesMem);
        clReleaseMemObject(attractiveForcesMem);
        clReleaseMemObject(displacementsMem);
        clReleaseMemObject(edgesMem);
        clReleaseMemObject(weightsMem);
        clReleaseKernel(kernelRepulsive);
        clReleaseKernel(kernelAttractive);
        clReleaseKernel(kernelUpdate);
        clReleaseKernel(kernelSummarize);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);

        return result;
    }
}
