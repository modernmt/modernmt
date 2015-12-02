package eu.modernmt.decoder;

import eu.modernmt.model.Sentence;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class Translation extends Sentence {

    private List<TranslationHypothesis> nbestList;

    public Translation(String rawText, List<TranslationHypothesis> nbestList) {
        super(rawText);
        this.nbestList = nbestList;
    }

    public Translation(List<String> tokens, List<TranslationHypothesis> nbestList) {
        super(tokens);
        this.nbestList = nbestList;
    }

    public Translation(String[] tokens, List<TranslationHypothesis> nbestList) {
        super(tokens);
        this.nbestList = nbestList;
    }

    public List<TranslationHypothesis> getNBestList() {
        return nbestList;
    }

}
