package eu.modernmt.model;

/**
 * Created by lucamastrostefano on 01/04/16.
 */
public class AutomaticTaggedTranslation extends Translation {

    private String automaticTaggedTranslation;

    public AutomaticTaggedTranslation(Word[] words, Sentence source, int[][] alignment) {
        super(words, source, alignment);
    }

    public AutomaticTaggedTranslation(Word[] words, Tag[] tags, Sentence source, int[][] alignment) {
        super(words, tags, source, alignment);
    }

    public String getAutomaticTaggedTranslation() {
        return automaticTaggedTranslation;
    }

    public void setAutomaticTaggedTranslation(String automaticTaggedTranslation) {
        this.automaticTaggedTranslation = automaticTaggedTranslation;
    }
}
