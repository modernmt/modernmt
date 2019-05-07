package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataListener;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by davide on 23/05/17.
 */
public interface TranslationMemory extends Closeable, DataListener {

    ScoreEntry[] search(UUID user, LanguagePair direction, Sentence source, ContextVector contextVector, int limit) throws IOException;

}
