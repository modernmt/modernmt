package eu.modernmt.decoder.opennmt.storage;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.Corpus;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public interface SuggestionStorage extends Closeable, DataListener {

    void add(Domain domain, Corpus corpus) throws StorageException;

    void add(Map<Domain, Corpus> corpora) throws StorageException;

    List<Suggestion> getSuggestions(Sentence source, int limit) throws StorageException;

    List<Suggestion> getSuggestions(Sentence source, ContextVector contextVector, int limit) throws StorageException;

    @Override
    void onDelete(Deletion deletion) throws StorageException;

    @Override
    void onDataReceived(List<TranslationUnit> batch) throws StorageException;

    @Override
    Map<Short, Long> getLatestChannelPositions();

}
