package eu.virtualparadox.springembedder;

import eu.virtualparadox.springembedder.renderercallback.NoOpRendererCallback;
import eu.virtualparadox.springembedder.renderercallback.AbstractRendererCallback;
import org.jgrapht.Graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Class for performing layout calculations using the Spring Embedder algorithm.
 *
 * @param <V> Type of the vertices in the graph.
 * @param <E> Type of the edges in the graph.
 */
public class FruchtermanReingoldLayouter<V, E> {

    private static final double C = 0.01;
    private final int width;
    private final int height;
    private final AbstractRendererCallback<V, E> callback;
    private final EdgeWeightNormalizer<V, E> edgeWeightNormalizer;
    private final Random random;


    /**
     * Constructor for the SpringEmbedderLayouter.
     *
     * @param width    Width of the layout area.
     * @param height   Height of the layout area.
     * @param callback Callback to render the layout at each iteration.
     */
    public FruchtermanReingoldLayouter(final int width,
                                       final int height,
                                       final AbstractRendererCallback<V, E> callback) {
        this.width = width;
        this.height = height;
        this.callback = Objects.requireNonNull(callback);
        this.edgeWeightNormalizer = new EdgeWeightNormalizer<>();
        this.random = new Random();
    }

    /**
     * Constructor for the SpringEmbedderLayouter.
     *
     * @param width  Width of the layout area.
     * @param height Height of the layout area.
     */
    public FruchtermanReingoldLayouter(final int width,
                                       final int height) {
        this(width, height, new NoOpRendererCallback<>());
    }

    /**
     * Main method to perform the layout.
     *
     * @param graph      The graph to layout.
     * @param iterations Number of iterations to perform.
     * @return The final positions of the nodes.
     */
    public Map<V, Vector2D> layout(final Graph<V, E> graph,
                                   final int iterations) {
        Map<V, Vector2D> positions = setInitialPositions(graph);
        double temperature = 50;
        final Map<E, Double> normalizedWeights = edgeWeightNormalizer.normalizeEdgeWeights(graph);

        for (int i = 0; i < iterations; i++) {
            positions = computeForcesAndUpdatePositions(graph, positions, normalizedWeights, temperature);
            callback.render(graph, i, positions);
            temperature = Math.max(1.5, temperature * 0.95);
        }

        callback.finish();
        return positions;
    }

    /**
     * Set initial positions for the nodes randomly within a defined area.
     *
     * @param graph The graph.
     * @return A map with the initial positions of the nodes.
     */
    private Map<V, Vector2D> setInitialPositions(final Graph<V, E> graph) {
        final Map<V, Vector2D> result = new HashMap<>();
        for (V v : graph.vertexSet()) {
            final double x = random.nextInt(width);
            final double y = random.nextInt(height);
            final Vector2D position = new Vector2D(x, y);
            result.put(v, position);
        }
        return result;
    }

    /**
     * Compute forces and update positions for all vertices.
     *
     * @param graph             The graph.
     * @param positions         The current positions of the nodes.
     * @param normalizedWeights The normalized edge weights.
     * @param temperature       The current temperature.
     * @return Updated positions of the nodes.
     */
    private Map<V, Vector2D> computeForcesAndUpdatePositions(final Graph<V, E> graph,
                                                             final Map<V, Vector2D> positions,
                                                             final Map<E, Double> normalizedWeights,
                                                             final double temperature) {
        final Map<V, Vector2D> displacements = computeForces(graph, positions, normalizedWeights);
        return updatePositions(graph, positions, displacements, temperature);
    }

    /**
     * Compute the forces for all vertices.
     *
     * @param graph             The graph.
     * @param positions         The current positions of the nodes.
     * @param normalizedWeights The normalized edge weights.
     * @return A map with the computed forces for each vertex.
     */
    private Map<V, Vector2D> computeForces(final Graph<V, E> graph,
                                           final Map<V, Vector2D> positions,
                                           final Map<E, Double> normalizedWeights) {
        final Map<V, Vector2D> repulsiveDisplacements = calculateRepulsiveForces(graph, positions);
        final Map<V, Vector2D> attractiveDisplacements = calculateAttractiveForces(graph, positions, normalizedWeights);
        return summarizeDisplacementVectors(repulsiveDisplacements, attractiveDisplacements);
    }

    /**
     * Summarize the repulsive and attractive forces for each vertex.
     *
     * @param repulsiveDisplacements  The repulsive forces.
     * @param attractiveDisplacements The attractive forces.
     * @return A map with the summarized forces for each vertex.
     */
    private Map<V, Vector2D> summarizeDisplacementVectors(final Map<V, Vector2D> repulsiveDisplacements,
                                                          final Map<V, Vector2D> attractiveDisplacements) {
        final Map<V, Vector2D> result = new HashMap<>();
        for (V v : repulsiveDisplacements.keySet()) {
            final Vector2D repulsive = repulsiveDisplacements.get(v);
            final Vector2D attractive = attractiveDisplacements.get(v);
            result.put(v, repulsive.add(attractive));
        }
        return result;
    }

    /**
     * Calculate the repulsive forces between all pairs of vertices.
     *
     * @param graph     The graph.
     * @param positions The current positions of the nodes.
     * @return A map with the repulsive forces for each vertex.
     */
    private Map<V, Vector2D> calculateRepulsiveForces(final Graph<V, E> graph,
                                                      final Map<V, Vector2D> positions) {
        final Map<V, Vector2D> result = initDisplacementVector(graph);
        double optimalDistance = calcOptimalDistance(graph);

        for (V v : graph.vertexSet()) {
            final Vector2D posV = positions.get(v);
            for (V u : graph.vertexSet()) {
                if (!v.equals(u)) {
                    final Vector2D posU = positions.get(u);
                    final Vector2D delta = posV.subtract(posU);
                    final double distance = delta.length();

                    if (distance > 0) {
                        double repulsion = C * (optimalDistance * optimalDistance) / distance;
                        Vector2D force = delta.normalize().scale(repulsion);
                        result.put(v, result.get(v).add(force));
                        result.put(u, result.get(u).subtract(force));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Calculate the attractive forces between connected vertices.
     *
     * @param graph             The graph.
     * @param positions         The current positions of the nodes.
     * @param normalizedWeights The normalized edge weights.
     * @return A map with the attractive forces for each vertex.
     */
    private Map<V, Vector2D> calculateAttractiveForces(final Graph<V, E> graph,
                                                       final Map<V, Vector2D> positions,
                                                       final Map<E, Double> normalizedWeights) {
        final Map<V, Vector2D> result = initDisplacementVector(graph);
        final int optimalDistance = (int) calcOptimalDistance(graph);

        for (E edge : graph.edgeSet()) {
            final V fromVertex = graph.getEdgeSource(edge);
            final V toVertex = graph.getEdgeTarget(edge);

            final Vector2D from = positions.get(fromVertex);
            final Vector2D to = positions.get(toVertex);

            final Vector2D delta = from.subtract(to);
            double distance = delta.length();

            if (distance > 0) {
                double normalizedWeight = normalizedWeights.get(edge);
                double attraction = C * (distance * distance) / optimalDistance * normalizedWeight;
                Vector2D force = delta.normalize().scale(attraction);
                result.put(fromVertex, result.get(fromVertex).subtract(force));
                result.put(toVertex, result.get(toVertex).add(force));
            }
        }

        return result;
    }

    /**
     * Initializes displacement vector map for all vertices.
     *
     * @param graph The graph.
     * @return A map with zero displacement vectors for each vertex.
     */
    private Map<V, Vector2D> initDisplacementVector(final Graph<V, E> graph) {
        final Map<V, Vector2D> displacement = new HashMap<>();
        for (V v : graph.vertexSet()) {
            displacement.put(v, new Vector2D(0, 0));
        }
        return displacement;
    }

    /**
     * Calculate optimal distance between nodes.
     *
     * @param graph The graph.
     * @return The optimal distance.
     */
    private double calcOptimalDistance(final Graph<V, E> graph) {
        return Math.sqrt((width * height) * 1.0d / graph.vertexSet().size()) / 2;
    }

    /**
     * Update the positions of the nodes based on the calculated forces.
     *
     * @param graph         The graph.
     * @param positions     The current positions of the nodes.
     * @param displacements The calculated forces for each node.
     * @param temperature   The current temperature.
     * @return Updated positions of the nodes.
     */
    private Map<V, Vector2D> updatePositions(final Graph<V, E> graph,
                                             final Map<V, Vector2D> positions,
                                             final Map<V, Vector2D> displacements,
                                             final double temperature) {
        final Map<V, Vector2D> result = new HashMap<>();

        for (V v : graph.vertexSet()) {
            // Get displacement of the vertex
            final Vector2D pos = positions.get(v);
            Vector2D displacement = displacements.get(v);

            // Downscale displacement vector to the temperature
            double displacementLength = displacement.length();
            displacement = displacement.scale(Math.min(displacementLength, temperature) / displacementLength);
            final Vector2D newPos = pos.add(displacement);

            // Bound the position within the layout area
            double newX = Math.max(0, Math.min(width, newPos.getX()));
            double newY = Math.max(0, Math.min(height, newPos.getY()));
            result.put(v, new Vector2D(newX, newY));
        }

        return result;
    }
}
