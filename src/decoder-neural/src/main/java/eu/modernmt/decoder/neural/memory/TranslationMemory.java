package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public interface TranslationMemory extends Closeable, DataListener {

    interface DataFilter {

        boolean accept(TranslationUnit unit);

    }

    /* This method does not store segments hash. Update of content inserted with this method is not possible */
    void bulkInsert(Map<Memory, MultilingualCorpus> batch) throws IOException;

    /* This method does not store segments hash. Update of content inserted with this method is not possible */
    void bulkInsert(Memory memory, MultilingualCorpus corpus) throws IOException;

    ScoreEntry[] search(LanguagePair direction, Sentence source, int limit) throws IOException;

    ScoreEntry[] search(LanguagePair direction, Sentence source, ContextVector contextVector, int limit) throws IOException;

    void setDataFilter(DataFilter filter);

}
