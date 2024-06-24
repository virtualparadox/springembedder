package eu.virtualparadox.springembedder.renderercallback;

import eu.virtualparadox.springembedder.Vector2D;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public abstract class AbstractRendererCallback<V, E> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Path outputFolder;
    protected final int width;
    protected final int height;

    protected AbstractRendererCallback(final Path outputFolder,
                                       final int width,
                                       final int height) {
        this.outputFolder = outputFolder;
        this.width = width;
        this.height = height;
    }

    public abstract void render(final DirectedWeightedPseudograph<V, E> graph,
                                final int iteration,
                                final Map<V, Vector2D> positionMap);

    public abstract void finish();
}
