package eu.virtualparadox.springembedder;

import eu.virtualparadox.springembedder.renderercallback.AbstractRendererCallback;
import eu.virtualparadox.springembedder.renderercallback.VideoRendererCallback;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    public static void main(String[] args) {
        final Path tempFolder = resolveTempFolder();

        final Graph<String, DefaultWeightedEdge> graph = DemoGraphInitializer.initializeDemoGraph(10, 10);

        final AbstractRendererCallback<String, DefaultWeightedEdge> callback = new VideoRendererCallback<>(tempFolder, WIDTH, HEIGHT);
        final FruchtermanReingoldLayouter<String, DefaultWeightedEdge> layouter = new FruchtermanReingoldLayouter<>(WIDTH, HEIGHT, callback);

        layouter.layout(graph, 1000);
    }

    private static Path resolveTempFolder() {
        final String tempFolder = System.getProperty("java.io.tmpdir");
        if (tempFolder != null) {
            return Paths.get(tempFolder);
        }
        throw new IllegalStateException("Failed to resolve temp folder");
    }
}