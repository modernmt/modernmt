package eu.modernmt.processing.builder;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.ProcessingPipeline;
import eu.modernmt.processing.TextProcessor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
public abstract class PipelineBuilder<P, R> {

    private final List<AbstractBuilder> builders;
    private final Class<ProcessingPipeline> pipelineClass;

    protected PipelineBuilder(List<AbstractBuilder> builders, Class<ProcessingPipeline> pipelineClass) {
        this.builders = builders;
        this.pipelineClass = pipelineClass;
    }

    @SuppressWarnings("unchecked")
    public final ProcessingPipeline<P, R> newPipeline(Locale source, Locale target) throws ProcessingException {
        ArrayList<TextProcessor> processors = new ArrayList<>(builders.size());

        for (AbstractBuilder builder : builders) {
            if (builder.accept(source, target))
                processors.add(builder.create(source, target));
        }

        try {
            return (ProcessingPipeline<P, R>) pipelineClass.getConstructor(List.class).newInstance(processors);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ProcessingException("Failed to instantiate class " + pipelineClass, e);
        }
    }

}
