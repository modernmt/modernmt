package eu.modernmt.processing.builder;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.ProcessingPipeline;
import eu.modernmt.processing.TextProcessor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 31/05/16.
 */
public abstract class PipelineBuilder<P, R> {

    private final List<AbstractBuilder> builders;
    private final Class<? extends ProcessingPipeline<P, R>> pipelineClass;

    protected PipelineBuilder(List<AbstractBuilder> builders, Class<? extends ProcessingPipeline<P, R>> pipelineClass) {
        this.builders = builders;
        this.pipelineClass = pipelineClass;
    }

    public final ProcessingPipeline<P, R> newPipeline(Language source, Language target) {
        ArrayList<TextProcessor<?, ?>> processors = new ArrayList<>(builders.size());

        for (AbstractBuilder builder : builders) {
            if (builder.accept(source, target))
                processors.add(builder.create(source, target));
        }

        try {
            return pipelineClass.getConstructor(List.class).newInstance(processors);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new Error("Failed to instantiate class " + pipelineClass, e);
        }
    }

}
