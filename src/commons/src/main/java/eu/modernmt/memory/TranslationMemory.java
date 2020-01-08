package eu.modernmt.memory;

import eu.modernmt.data.LogDataListener;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by davide on 23/05/17.
 */
public interface TranslationMemory extends Closeable, LogDataListener {

    class Entry {

        public final long memory;
        public final LanguageDirection language;
        public final String sentence;
        public final String translation;

        public Entry(long memory, LanguageDirection language, String sentence, String translation) {
            this.memory = memory;
            this.language = language;
            this.sentence = sentence;
            this.translation = translation;
        }

    }

    ScoreEntry[] search(UUID user, LanguageDirection direction, Sentence source, ContextVector contextVector, int limit) throws IOException;

    void optimize() throws IOException;

    int size();

    void dumpAll(Consumer<Entry> consumer) throws IOException;

    void dump(long memory, Consumer<Entry> consumer) throws IOException;

}
