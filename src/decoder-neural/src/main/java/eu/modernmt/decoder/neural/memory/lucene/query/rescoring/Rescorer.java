package eu.modernmt.decoder.neural.memory.lucene.query.rescoring;

import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 06/08/17.
 */
public interface Rescorer {

    ScoreEntry[] rescore(LanguageDirection direction, Sentence input, ScoreEntry[] entries, ContextVector context);

}
