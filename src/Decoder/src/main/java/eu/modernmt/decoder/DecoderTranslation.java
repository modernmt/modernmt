package eu.modernmt.decoder;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class DecoderTranslation extends Translation {

    protected List<TranslationHypothesis> nbest;

    public DecoderTranslation(Word[] words, Sentence source, int[][] alignment) {
        super(words, source, alignment);
    }

    public DecoderTranslation(Word[] words, Tag[] tags, Sentence source, int[][] alignment) {
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
