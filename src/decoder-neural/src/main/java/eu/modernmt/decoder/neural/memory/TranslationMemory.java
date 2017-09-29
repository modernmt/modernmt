package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataListener;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public interface TranslationMemory extends Closeable, DataListener {

    void add(Map<Memory, MultilingualCorpus> batch) throws IOException;

    void add(Memory memory, MultilingualCorpus corpus) throws IOException;

    void add(LanguagePair direction, Memory memory, Sentence sentence, Sentence translation) throws IOException;

    ScoreEntry[] search(LanguagePair direction, Sentence source, int limit) throws IOException;

    ScoreEntry[] search(LanguagePair direction, Sentence source, ContextVector contextVector, int limit) throws IOException;

}
