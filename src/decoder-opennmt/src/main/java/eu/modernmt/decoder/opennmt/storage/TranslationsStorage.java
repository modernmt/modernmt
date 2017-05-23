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

    class Entry {

        public final int domain;
        public final String[] sentence;
        public final String[] translation;

        public Entry(int domain, String[] sentence, String[] translation) {
            this.domain = domain;
            this.sentence = sentence;
            this.translation = translation;
        }

    }

    class SearchResult {

        public final Entry[] entries;
        public final float[] scores;

        public SearchResult(Entry[] entries, float[] scores) {
            this.entries = entries;
            this.scores = scores;
        }

        public boolean isEmpty() {
            return entries.length == 0;
        }

        public int size() {
            return entries.length;
        }

    }

    void add(Domain domain, BilingualCorpus corpus) throws StorageException, IOException;

    void add(Domain domain, Sentence sentence, Sentence translation) throws StorageException;

    SearchResult search(Sentence source, int limit) throws StorageException;

    SearchResult search(Sentence source, ContextVector contextVector, int limit) throws StorageException;

}
