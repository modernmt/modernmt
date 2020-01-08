package eu.modernmt.memory;

import eu.modernmt.data.LogDataListener;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by davide on 23/05/17.
 */
public interface TranslationMemory extends Closeable, LogDataListener {

    ScoreEntry[] search(UUID user, LanguageDirection direction, Sentence source, ContextVector contextVector, int limit) throws IOException;

    void optimize() throws IOException;

    int size();

}
