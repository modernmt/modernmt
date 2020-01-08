package eu.modernmt.processing.tokenizer.languagetool.tiny;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Tokenizes a sentence into words. Punctuation and whitespace gets its own token.
 *
 * @author Daniel Naber
 */
public class MalayalamWordTokenizer implements LanguageToolTokenizer {

    public MalayalamWordTokenizer() {
    }

    @Override
    public List<String> tokenize(final String text) {
        final List<String> tokens = new ArrayList<>();
        final StringTokenizer st = new StringTokenizer(text,
                "\u0020\u00A0\u115f\u1160\u1680"
                        + ",.;()[]{}!?:\"'’‘„“”…\\/\t\n", true);
        while (st.hasMoreElements()) {
            tokens.add(st.nextToken());
        }
        return tokens;
    }

}

