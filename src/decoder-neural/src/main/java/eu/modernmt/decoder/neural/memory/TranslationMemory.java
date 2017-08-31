package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataListener;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
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

    void add(Map<Domain, MultilingualCorpus> batch) throws IOException;

    void add(Domain domain, MultilingualCorpus corpus) throws IOException;

    void add(LanguagePair direction, Domain domain, Sentence sentence, Sentence translation) throws IOException;

    ScoreEntry[] search(LanguagePair direction, Sentence source, int limit) throws IOException;

    ScoreEntry[] search(LanguagePair direction, Sentence source, ContextVector contextVector, int limit) throws IOException;

}
