package eu.virtualparadox.springembedder.renderercallback;

import eu.virtualparadox.springembedder.Vector2D;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class VideoRendererCallback<V, E> extends PngRendererCallback<V, E> {

    private AWTSequenceEncoder encoder;

    public VideoRendererCallback(final Path outputFolder,
                                 final int width,
                                 final int height) {
        super(outputFolder, width, height);
        try {
            encoder = AWTSequenceEncoder.create24Fps(outputFolder.resolve("output.mp4").toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create encoder", e);
        }
    }

    @Override
    public void render(final DirectedWeightedPseudograph<V, E> graph,
                       final int iteration,
                       final Map<V, Vector2D> positions) {

        try {
            final BufferedImage image = renderToImage(graph, iteration, positions);
            encoder.encodeImage(image);
            if (iteration % 100 == 0) {
                logger.info("Rendered frame {}", iteration);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render frame", e);
        }
    }

    @Override
    public void finish() {
        try {
            logger.info("Finishing encoding and saving video to {}/output.mp4", outputFolder.toAbsolutePath());
            encoder.finish();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to finish encoding", e);
        }
    }
}
