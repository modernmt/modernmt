package eu.modernmt.decoder.opennmt.memory;

import eu.modernmt.data.DataListener;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public interface TranslationMemory extends Closeable, DataListener {

    void add(Map<Domain, BilingualCorpus> batch) throws IOException;

    void add(Domain domain, BilingualCorpus corpus) throws IOException;

    void add(Domain domain, Sentence sentence, Sentence translation) throws IOException;

    ScoreEntry[] search(Sentence source, int limit) throws IOException;

    ScoreEntry[] search(Sentence source, ContextVector contextVector, int limit) throws IOException;

}
