package eu.modernmt.rest.model;

import eu.modernmt.model.Alignment;

/**
 * Created by davide on 01/09/16.
 */
public class ProjectedTranslation {

    private final String translation;
    private String[] sourceTokens;
    private String[] targetTokens;
    private Alignment alignment;

    public ProjectedTranslation(String translation) {
        this.translation = translation;
    }

    public String getTranslation() {
        return translation;
    }

    public String[] getSourceTokens() {
        return sourceTokens;
    }

    public String[] getTargetTokens() {
        return targetTokens;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setSourceTokens(String[] sourceTokens) {
        this.sourceTokens = sourceTokens;
    }

    public void setTargetTokens(String[] targetTokens) {
        this.targetTokens = targetTokens;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }
}
