package eu.modernmt.decoder;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class DecoderTranslation extends Translation {

    protected List<TranslationHypothesis> nbest;

    public DecoderTranslation(Token[] tokens, Sentence source, int[][] alignment) {
        super(tokens, source, alignment);
    }

    public DecoderTranslation(Token[] tokens, Tag[] tags, Sentence source, int[][] alignment) {
        super(tokens, tags, source, alignment);
    }

    public List<TranslationHypothesis> getNbest() {
        return nbest;
    }

    public boolean hasNbest() {
        return nbest != null && nbest.size() > 0;
    }

    public void setNbest(List<TranslationHypothesis> nbest) {
        this.nbest = nbest;
    }

}
