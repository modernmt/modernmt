package eu.modernmt.decoder.opennmt.storage;

import eu.modernmt.data.DataListener;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by davide on 23/05/17.
 */
public interface TranslationsStorage extends Closeable, DataListener {

    void add(Domain domain, BilingualCorpus corpus) throws StorageException, IOException;

    void add(Domain domain, Sentence sentence, Sentence translation) throws StorageException;

    ScoreEntry[] search(Sentence source, int limit) throws StorageException;

    ScoreEntry[] search(Sentence source, ContextVector contextVector, int limit) throws StorageException;

}
