package eu.modernmt.decoder;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class Translation extends Sentence {

    protected Sentence source;
    protected List<TranslationHypothesis> nbest;

    public Translation(String rawText, Sentence source) {
        super(rawText);
        this.source = source;
    }

    public Translation(List<String> tokens, Sentence source) {
        super(tokens);
        this.source = source;
    }

    public Translation(String[] tokens, Sentence source) {
        super(tokens);
        this.source = source;
    }

    public Sentence getSource() {
        return source;
    }

    public List<TranslationHypothesis> getNbest() {
        return nbest;
    }

    public void setNbest(List<TranslationHypothesis> nbest) {
        this.nbest = nbest;
    }

}
