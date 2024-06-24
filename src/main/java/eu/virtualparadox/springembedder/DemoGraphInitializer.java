package eu.virtualparadox.springembedder;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;

public class DemoGraphInitializer {

    public static final String CENTER = "center";

    private DemoGraphInitializer() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static DirectedWeightedPseudograph<String, DefaultWeightedEdge> initializeDemoGraph(int centers, int nodesPerCenter) {
        final DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        graph.addVertex(CENTER);
        for (int c = 0; c < centers; c++) {
            final String center = CENTER + c;
            graph.addVertex(center);
            addEdge(graph, CENTER, center, 10);
        }

        for (int c = 0; c < centers; c++) {
            for (int i = 0; i < nodesPerCenter; i++) {
                final String node = "node_" + c + "_" + i;
                graph.addVertex(node);
                addEdge(graph, CENTER + c, node, 1);
            }
        }

        return graph;
    }

    private static void addEdge(final DirectedWeightedPseudograph<String, DefaultWeightedEdge> graph,
                                final String source,
                                final String target,
                                final double weight) {
        DefaultWeightedEdge edge = graph.addEdge(source, target);
        if (edge != null) {
            graph.setEdgeWeight(edge, weight);
        }
    }
}
