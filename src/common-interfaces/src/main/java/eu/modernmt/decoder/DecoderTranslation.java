package eu.modernmt.decoder;

import eu.modernmt.model.*;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class DecoderTranslation extends Translation {

    protected List<TranslationHypothesis> nbest;

    public DecoderTranslation(Word[] words, Sentence source, Alignment alignment) {
        super(words, source, alignment);
    }

    public DecoderTranslation(Word[] words, Tag[] tags, Sentence source, Alignment alignment) {
        super(words, tags, source, alignment);
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
