package eu.modernmt.processing.framework;

import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
public abstract class PipelineFactory<P, R> {



    public abstract ProcessingPipeline<P, R> newPipeline(Locale source, Locale target) throws ProcessingException;

}
