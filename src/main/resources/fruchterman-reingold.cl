// Calculate Repulsive Forces Kernel
__kernel void calculateRepulsiveForces(
    __global const float* positions,
    __global float* repulsiveMatrix,
    int numVertices,
    float optimalDistance,
    float C)
{
    int i = get_global_id(0);
    if (i >= numVertices) return;

    // Zero-initialize the repulsive matrix for this vertex
    for (int j = 0; j < numVertices; j++) {
        repulsiveMatrix[2 * (i * numVertices + j)] = 0.0f;
        repulsiveMatrix[2 * (i * numVertices + j) + 1] = 0.0f;
    }

    float2 posV = (float2)(positions[2 * i], positions[2 * i + 1]);

    for (int j = 0; j < numVertices; j++) {
        if (i != j) {
            float2 posU = (float2)(positions[2 * j], positions[2 * j + 1]);
            float2 delta = posV - posU;
            float distance = length(delta);
            if (distance > 0) {
                float force = C * (optimalDistance * optimalDistance) / distance;
                float2 repulsiveForce = normalize(delta) * force;
                repulsiveMatrix[2 * (i * numVertices + j)] = repulsiveForce.x;
                repulsiveMatrix[2 * (i * numVertices + j) + 1] = repulsiveForce.y;
            }
        }
    }
}

// Calculate Attractive Forces Kernel
__kernel void calculateAttractiveForces(
    __global const float* positions,
    __global float* attractiveMatrix,
    __global const int2* edges,
    __global const float* weights,
    int numEdges,
    int numVertices,
    float optimalDistance,
    float C)
{
    int i = get_global_id(0);
    if (i >= numEdges) return;

    // Zero-initialize the attractive matrix for this edge
    for (int v = 0; v < numVertices; v++) {
        attractiveMatrix[2 * (i * numVertices + v)] = 0.0f;
        attractiveMatrix[2 * (i * numVertices + v) + 1] = 0.0f;
    }

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

        attractiveMatrix[2 * (i * numVertices + from)] = -attractiveForce.x;
        attractiveMatrix[2 * (i * numVertices + from) + 1] = -attractiveForce.y;
        attractiveMatrix[2 * (i * numVertices + to)] = attractiveForce.x;
        attractiveMatrix[2 * (i * numVertices + to) + 1] = attractiveForce.y;
    }
}

// Summarize Forces Kernel
__kernel void summarizeForces(
    __global const float* repulsiveMatrix,
    __global const float* attractiveMatrix,
    __global float* displacements,
    int numVertices,
    int numEdges)
{
    int v = get_global_id(0);
    if (v >= numVertices) return;

    // Zero-initialize the displacements for this vertex
    float dx = 0.0f;
    float dy = 0.0f;

    // Summarize repulsive forces
    for (int j = 0; j < numVertices; j++) {
        if (v != j) {
            dx += repulsiveMatrix[2 * (v * numVertices + j)];
            dy += repulsiveMatrix[2 * (v * numVertices + j) + 1];
        }
    }

    // Summarize attractive forces
    for (int e = 0; e < numEdges; e++) {
        dx += attractiveMatrix[2 * (e * numVertices + v)];
        dy += attractiveMatrix[2 * (e * numVertices + v) + 1];
    }

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
