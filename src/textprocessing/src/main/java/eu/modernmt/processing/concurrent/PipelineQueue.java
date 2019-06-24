package eu.modernmt.processing.concurrent;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.ProcessingPipeline;
import eu.modernmt.processing.builder.PipelineBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by davide on 01/08/17.
 */
class PipelineQueue<P, R> {

    private final ConcurrentHashMap<LanguageDirection, ConcurrentLinkedQueue<ProcessingPipeline<P, R>>> pipelines = new ConcurrentHashMap<>();
    private final PipelineBuilder<P, R> builder;

    PipelineQueue(PipelineBuilder<P, R> builder) {
        this.builder = builder;
    }

    public ProcessingPipeline<P, R> get(LanguageDirection language) throws ProcessingException {
        ProcessingPipeline<P, R> pipeline = pipelines
                .computeIfAbsent(language, k -> new ConcurrentLinkedQueue<>())
                .poll();

        if (pipeline == null)
            pipeline = builder.newPipeline(language.source, language.target);

        return pipeline;
    }

    public void release(LanguageDirection language, ProcessingPipeline<P, R> pipeline) {
        pipelines.computeIfAbsent(language, k -> new ConcurrentLinkedQueue<>())
                .offer(pipeline);
    }

}
