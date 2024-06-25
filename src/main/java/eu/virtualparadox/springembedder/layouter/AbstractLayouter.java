package eu.virtualparadox.springembedder.layouter;

import eu.virtualparadox.springembedder.EdgeWeightNormalizer;
import eu.virtualparadox.springembedder.Vector2D;
import eu.virtualparadox.springembedder.renderercallback.AbstractRendererCallback;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;

public abstract class AbstractLayouter<V, E> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final int width;
    protected final int height;
    protected final AbstractRendererCallback<V, E> callback;
    protected final EdgeWeightNormalizer<V, E> edgeWeightNormalizer;
    protected final Random random;

    protected AbstractLayouter(int width, int height, AbstractRendererCallback<V, E> callback) {
        this.width = width;
        this.height = height;
        this.callback = callback;
        this.edgeWeightNormalizer = new EdgeWeightNormalizer<>();
        this.random = new Random(1);
    }

    public abstract Map<V, Vector2D> layout(Graph<V, E> graph, int i);
}
