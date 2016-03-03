package eu.modernmt.processing.framework;

import java.io.Closeable;

/**
 * Created by davide on 26/01/16.
 */
public interface TextProcessor<P, R> extends Closeable {

    R call(P param) throws ProcessingException;

}
