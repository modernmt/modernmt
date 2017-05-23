package eu.modernmt.decoder.opennmt.storage;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

/**
 * Created by davide on 23/05/17.
 */
public class Suggestion {

    public final Sentence source;
    public final Translation translation;
    public final float score;

    public Suggestion(Sentence source, Translation translation, float score) {
        this.source = source;
        this.translation = translation;
        this.score = score;
    }
}
