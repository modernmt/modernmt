package eu.modernmt.context;

import eu.modernmt.model.corpus.Corpus;

import java.io.Closeable;
import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * Created by davide on 02/12/15.
 */
public interface ContextAnalyzer extends Closeable {

    void add(Corpus corpus) throws ContextAnalyzerException;

    void add(Collection<Corpus> corpora) throws ContextAnalyzerException;

    List<ContextScore> getContext(String query, int limit) throws ContextAnalyzerException;

    List<ContextScore> getContext(File source, int limit) throws ContextAnalyzerException;

    List<ContextScore> getContext(Corpus query, int limit) throws ContextAnalyzerException;

}
