package eu.virtualparadox.springembedder;

import org.jgrapht.Graph;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for normalizing edge weights in a directed weighted graph.
 *
 * @param <V> the type of the vertices in the graph.
 * @param <E> the type of the edges in the graph.
 */
public class EdgeWeightNormalizer<V, E> {

    private static final double MIN_NORMALIZED_WEIGHT = 1.0;
    private static final double MAX_NORMALIZED_WEIGHT = 10.0;

    /**
     * Normalizes the edge weights of the given graph to a specified range.
     * The normalization process scales the edge weights to fit within the range [1, 10].
     *
     * @param graph the graph whose edge weights are to be normalized.
     * @return a map where the keys are the edges of the graph and the values are the normalized weights.
     */
    public Map<E, Double> normalizeEdgeWeights(final Graph<V, E> graph) {
        final Map<E, Double> normalizedWeights = new HashMap<>();

        // if the graph is not weighted, assign a weight of 1.0 to all edges
        if (!graph.getType().isWeighted()) {
            for (E edge : graph.edgeSet()) {
                normalizedWeights.put(edge, 1.0);
            }
            return normalizedWeights;
        }

        // otherwise, normalize the edge weights
        double minWeight = Double.MAX_VALUE;
        double maxWeight = Double.MIN_VALUE;

        // Find the minimum and maximum edge weights in the graph.
        for (E edge : graph.edgeSet()) {
            double weight = graph.getEdgeWeight(edge);
            if (weight < minWeight) {
                minWeight = weight;
            }
            if (weight > maxWeight) {
                maxWeight = weight;
            }
        }

        // Normalize the edge weights to the range [1, 10].
        for (E edge : graph.edgeSet()) {
            double rawWeight = graph.getEdgeWeight(edge);
            double normalizationRange = MAX_NORMALIZED_WEIGHT - MIN_NORMALIZED_WEIGHT;
            double normalizedWeight = MIN_NORMALIZED_WEIGHT + normalizationRange * (rawWeight - minWeight) / (maxWeight - minWeight);
            normalizedWeights.put(edge, normalizedWeight);
        }

        return normalizedWeights;
    }
}
