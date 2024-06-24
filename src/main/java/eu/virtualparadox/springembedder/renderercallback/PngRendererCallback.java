package eu.virtualparadox.springembedder.renderercallback;

import eu.virtualparadox.springembedder.EdgeWeightNormalizer;
import eu.virtualparadox.springembedder.Vector2D;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;

public class PngRendererCallback<V, E> extends AbstractRendererCallback<V, E> {

    private Map<E, Double> normalizedWeights;

    public PngRendererCallback(final Path outputFolder,
                               final int width,
                               final int height) {
        super(outputFolder, width, height);
    }

    public void render(final DirectedWeightedPseudograph<V, E> graph,
                       final int iteration,
                       final Map<V, Vector2D> positions) {

        final BufferedImage image = renderToImage(graph, iteration, positions);
        try {
            final String step = StringUtils.leftPad(String.valueOf(iteration), 4, "0");
            ImageIO.write(image, "PNG", outputFolder.resolve("iteration-" + step + ".png").toFile());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render frame", e);
        }
    }

    protected BufferedImage renderToImage(final DirectedWeightedPseudograph<V, E> graph,
                                        final int iteration,
                                        final Map<V, Vector2D> positions) {
        // Normalize edge weights once,
        // okay, it's disgusting... needs to be refactored
        if (iteration == 0) {
            final EdgeWeightNormalizer<V, E> edgeWeightNormalizer = new EdgeWeightNormalizer<>();
            this.normalizedWeights = edgeWeightNormalizer.normalizeEdgeWeights(graph);
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        for (V from : graph.vertexSet()) {
            for (E edge : graph.outgoingEdgesOf(from)) {
                final V to = graph.getEdgeTarget(edge);
                final Vector2D fromPosition = positions.get(from);
                final Vector2D toPosition = positions.get(to);
                double normalizedWeight = normalizedWeights.get(edge);
                g.setStroke(new BasicStroke((float) normalizedWeight));
                g.setColor(Color.BLACK);
                g.drawLine((int) fromPosition.getX(), (int) fromPosition.getY(), (int) toPosition.getX(), (int) toPosition.getY());
            }
        }

        g.setColor(Color.RED);
        for (V v : graph.vertexSet()) {
            final Vector2D n = positions.get(v);
            g.fillOval((int) n.getX() - 5, (int) n.getY() - 5, 10, 10);
        }

        g.dispose();
        return image;
    }

    @Override
    public void finish() {
        logger.info("Files saved to {}", outputFolder.toAbsolutePath());
    }
}
