package eu.modernmt.processing.framework;

import java.util.LinkedList;
import java.util.Map;

/**
 * Created by davide on 26/01/16.
 */
public class VoidProcessingPipeline<P> extends ProcessingPipeline<P, Void> {

    protected VoidProcessingPipeline(LinkedList<TextProcessor<Object, Object>> processors) {
        super(processors);
    }

    public Void call(P input) throws ProcessingException {
        super.call(input);
        return null;
    }

    @SuppressWarnings("unchecked")
    public Void call(P input, Map<String, Object> metadata) throws ProcessingException {
        super.call(input, metadata);
        return null;
    }

}
