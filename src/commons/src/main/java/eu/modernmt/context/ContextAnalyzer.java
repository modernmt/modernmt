package eu.modernmt.context;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.LanguagePair;
import eu.modernmt.model.corpus.Corpus;

import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 02/12/15.
 */
public interface ContextAnalyzer extends Closeable, DataListener {

    void add(LanguagePair direction, Domain domain, Corpus corpus) throws ContextAnalyzerException;

    void add(LanguagePair direction, Map<Domain, Corpus> corpora) throws ContextAnalyzerException;

    ContextVector getContextVector(LanguagePair direction, String query, int limit) throws ContextAnalyzerException;

    ContextVector getContextVector(LanguagePair direction, File source, int limit) throws ContextAnalyzerException;

    ContextVector getContextVector(LanguagePair direction, Corpus query, int limit) throws ContextAnalyzerException;

    @Override
    void onDelete(Deletion deletion) throws ContextAnalyzerException;

    @Override
    void onDataReceived(List<TranslationUnit> batch) throws ContextAnalyzerException;

    @Override
    Map<Short, Long> getLatestChannelPositions();

}
