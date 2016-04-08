package eu.modernmt.processing.framework;

import java.io.Closeable;
import java.util.Map;

/**
 * Created by davide on 26/01/16.
 */
public interface TextProcessor<P, R> extends Closeable {

    R call(P param, Map<String, Object> metadata) throws ProcessingException;

}
