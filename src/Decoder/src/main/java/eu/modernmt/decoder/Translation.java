package eu.modernmt.decoder;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class Translation extends Sentence {

    private Sentence source;

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
}
