package eu.modernmt.decoder.opennmt.storage.lucene;

import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.opennmt.storage.StorageException;
import eu.modernmt.decoder.opennmt.storage.Suggestion;
import eu.modernmt.decoder.opennmt.storage.SuggestionStorage;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.Corpus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public class LuceneSuggestionStorage implements SuggestionStorage {

    public LuceneSuggestionStorage(File path) throws StorageException {

    }

    @Override
    public void add(Domain domain, Corpus corpus) throws StorageException {

    }

    @Override
    public void add(Map<Domain, Corpus> corpora) throws StorageException {

    }

    @Override
    public List<Suggestion> getSuggestions(Sentence source, int limit) throws StorageException {
        return null;
    }

    @Override
    public List<Suggestion> getSuggestions(Sentence source, ContextVector contextVector, int limit) throws StorageException {
        return null;
    }

    @Override
    public void onDelete(Deletion deletion) throws StorageException {

    }

    @Override
    public void onDataReceived(List<TranslationUnit> batch) throws StorageException {

    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

}
