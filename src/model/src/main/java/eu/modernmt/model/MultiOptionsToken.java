package eu.modernmt.model;

/**
 * Created by davide on 11/04/16.
 */
public interface MultiOptionsToken {

    String[] getSourceOptions();

    void setTranslatedOptions(Translation[] translations);

    boolean hasTranslatedOptions();

}
