package eu.modernmt.processing;

import java.util.List;
import java.util.Map;

/**
 * Created by davide on 26/01/16.
 */
public class ProcessingPipeline<P, R> {

    protected List<TextProcessor<Object, Object>> processors;

    public ProcessingPipeline(List<TextProcessor<Object, Object>> processors) {
        this.processors = processors;
    }

    /**
     * This method is NOT thread safe because the framework does not enforce
     * the TextProcessors to be thread safe.
     */
    @SuppressWarnings("unchecked")
    public R call(P input, Map<String, Object> metadata) throws ProcessingException {
        Object result = input;

        for (TextProcessor<Object, Object> processor : processors) {
            try {
                result = processor.call(result, metadata);
            } catch (RuntimeException e) {
                throw new ProcessingException(input, processor, e);
            }
        }

        return (R) result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < processors.size(); i++) {
            builder.append(processors.get(i).getClass().getSimpleName());
            if (i < processors.size() - 1)
                builder.append(", ");
        }
        builder.append(']');
        return builder.toString();
    }

}
