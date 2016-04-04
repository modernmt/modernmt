package eu.modernmt.model;

/**
 * Created by lucamastrostefano on 01/04/16.
 */
public class AutomaticTaggedTranslation extends Translation {

    private String automaticTaggedTranslation;

    public AutomaticTaggedTranslation(Token[] tokens, Sentence source, int[][] alignment) {
        super(tokens, source, alignment);
    }

    public AutomaticTaggedTranslation(Token[] tokens, Tag[] tags, Sentence source, int[][] alignment) {
        super(tokens, tags, source, alignment);
    }

    public String getAutomaticTaggedTranslation() {
        return automaticTaggedTranslation;
    }

    public void setAutomaticTaggedTranslation(String automaticTaggedTranslation) {
        this.automaticTaggedTranslation = automaticTaggedTranslation;
    }
}
