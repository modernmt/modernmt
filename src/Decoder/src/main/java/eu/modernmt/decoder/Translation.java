package eu.modernmt.decoder;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class Translation extends Sentence {

    protected Sentence source;
    protected List<TranslationHypothesis> nbest;
    protected int[][] alignment;

    public Translation(String rawText, Sentence source, int[][] alignment) {
        super(rawText);
        this.source = source;
        this.alignment = alignment;
    }

    public Translation(List<String> tokens, Sentence source, int[][] alignment) {
        super(tokens);
        this.source = source;
        this.alignment = alignment;
    }

    public Translation(String[] tokens, Sentence source, int[][] alignment) {
        super(tokens);
        this.source = source;
        this.alignment = alignment;
    }

    public int[][] getAlignment() {
        return alignment;
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
