package eu.modernmt.decoder.opennmt.memory.lucene.rescoring;

import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 06/08/17.
 */
public interface Rescorer {

    void rescore(Sentence input, ScoreEntry[] entries);

    void rescore(Sentence input, ScoreEntry[] entries, ContextVector context);

}
