package eu.virtualparadox.springembedder.renderercallback;

import eu.virtualparadox.springembedder.Vector2D;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import java.util.Map;

public class NoOpRendererCallback<V,E> extends AbstractRendererCallback<V,E> {

    public NoOpRendererCallback() {
        super(null, 0, 0);
    }

    @Override
    public void render(DirectedWeightedPseudograph<V, E> graph, int iteration, Map<V, Vector2D> positionMap) {
        // do nothing
    }

    @Override
    public void finish() {
        // do nothing
    }
}
