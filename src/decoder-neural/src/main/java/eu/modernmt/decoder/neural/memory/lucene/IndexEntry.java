package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.lang.LanguagePair;

/**
 * Created by davide on 12/02/18.
 */
public class IndexEntry {

    public final long memory;
    public final LanguagePair language;
    public final String[] sentence;
    public final String[] translation;

    public IndexEntry(long memory, LanguagePair language, String[] sentence, String[] translation) {
        this.memory = memory;
        this.language = language;
        this.sentence = sentence;
        this.translation = translation;
    }

}
