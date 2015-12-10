package eu.modernmt.decoder;

import java.io.Serializable;
import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class Sentence implements Serializable {

    protected String[] tokens;

    public Sentence(String rawText) {
        this(rawText.split("\\s+"));
    }

    public Sentence(List<String> tokens) {
        this(tokens.toArray(new String[tokens.size()]));
    }

    public Sentence(String[] tokens) {
        this.tokens = tokens;
    }

    public String[] getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        for (int i = 0; i < tokens.length; i++) {
            string.append(tokens[i]);
            if (i < tokens.length - 1)
                string.append(' ');
        }

        return string.toString();
    }
}
