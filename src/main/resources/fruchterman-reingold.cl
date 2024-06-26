// Constants
#define HACK_FACTOR 100000.0f // Hack to convert float to int because OpenCL 1.2 doesn't support float atomics

// Calculate Repulsive Forces Kernel
__kernel void calculateRepulsiveForces(
    __global const float* positions,
    __global int* repulsiveForces,
    int numVertices,
    float optimalDistance,
    float C)
{
    int i = get_global_id(0);
    if (i >= numVertices) return;

    float2 posV = (float2)(positions[2 * i], positions[2 * i + 1]);

    for (int j = 0; j < numVertices; j++) {
        if (i != j) {
            float2 posU = (float2)(positions[2 * j], positions[2 * j + 1]);
            float2 delta = posV - posU;
            float distance = length(delta);
            if (distance > 0) {
                float force = C * (optimalDistance * optimalDistance) / distance;
                float2 repulsiveForce = normalize(delta) * force;

                int intRepulsiveForceX = (int)(repulsiveForce.x * HACK_FACTOR);
                int intRepulsiveForceY = (int)(repulsiveForce.y * HACK_FACTOR);

                atomic_add(&repulsiveForces[2 * i], intRepulsiveForceX);
                atomic_add(&repulsiveForces[2 * i + 1], intRepulsiveForceY);
            }
        }
    }
}

// Calculate Attractive Forces Kernel
__kernel void calculateAttractiveForces(
    __global const float* positions,
    __global int* attractiveForces,
    __global const int2* edges,
    __global const float* weights,
    int numEdges,
    int numVertices,
    float optimalDistance,
    float C)
{
    int i = get_global_id(0);
    if (i >= numEdges) return;

    int2 edge = edges[i];
    int from = edge.x;
    int to = edge.y;
    float weight = weights[i];

    float2 fromPos = (float2)(positions[2 * from], positions[2 * from + 1]);
    float2 toPos = (float2)(positions[2 * to], positions[2 * to + 1]);
    float2 delta = fromPos - toPos;
    float distance = length(delta);

    if (distance > 0) {
        float forceMagnitude = C * weight * (distance * distance) / optimalDistance;
        float2 attractiveForce = normalize(delta) * forceMagnitude;

        int intAttractiveForceX = (int)(attractiveForce.x * HACK_FACTOR);
        int intAttractiveForceY = (int)(attractiveForce.y * HACK_FACTOR);

        atomic_add(&attractiveForces[2 * from], -intAttractiveForceX);
        atomic_add(&attractiveForces[2 * from + 1], -intAttractiveForceY);
        atomic_add(&attractiveForces[2 * to], intAttractiveForceX);
        atomic_add(&attractiveForces[2 * to + 1], intAttractiveForceY);
    }
}

// Summarize Forces Kernel
__kernel void summarizeForces(
    __global const int* repulsiveForces,
    __global const int* attractiveForces,
    __global float* displacements,
    int numVertices)
{
    int v = get_global_id(0);
    if (v >= numVertices) return;

    int repulsiveX = repulsiveForces[2 * v];
    int repulsiveY = repulsiveForces[2 * v + 1];
    int attractiveX = attractiveForces[2 * v];
    int attractiveY = attractiveForces[2 * v + 1];

    float dx = (repulsiveX + attractiveX) / HACK_FACTOR;
    float dy = (repulsiveY + attractiveY) / HACK_FACTOR;

    displacements[2 * v] = dx;
    displacements[2 * v + 1] = dy;
}

// Update Positions Kernel
__kernel void updatePositions(
    __global float* positions,
    __global const float* displacements,
    int numVertices,
    float temperature,
    int width,
    int height)
{
    int i = get_global_id(0);
    if (i >= numVertices) return;

    float2 pos = (float2)(positions[2 * i], positions[2 * i + 1]);
    float2 disp = (float2)(displacements[2 * i], displacements[2 * i + 1]);

    float dispLength = length(disp);
    if (dispLength > 0) {
        disp *= fmin(dispLength, temperature) / dispLength;
    }

    float2 newPos = pos + disp;
    newPos.x = fmin(width, fmax(0, newPos.x));
    newPos.y = fmin(height, fmax(0, newPos.y));

    positions[2 * i] = newPos.x;
    positions[2 * i + 1] = newPos.y;
}
