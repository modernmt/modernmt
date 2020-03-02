package eu.modernmt.context;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.corpus.Corpus;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by davide on 02/12/15.
 */
public interface ContextAnalyzer extends Closeable {

    ContextVector getContextVector(UUID user, LanguageDirection direction, String query, int limit) throws ContextAnalyzerException;

    ContextVector getContextVector(UUID user, LanguageDirection direction, File source, int limit) throws ContextAnalyzerException;

    ContextVector getContextVector(UUID user, LanguageDirection direction, Corpus query, int limit) throws ContextAnalyzerException;

    void optimize() throws ContextAnalyzerException;

}
