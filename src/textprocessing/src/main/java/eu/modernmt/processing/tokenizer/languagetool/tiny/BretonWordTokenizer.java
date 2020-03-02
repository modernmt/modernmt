package eu.modernmt.processing.tokenizer.languagetool.tiny;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Dominique Pelle
 */
public class BretonWordTokenizer extends WordTokenizer {

    public BretonWordTokenizer() {
    }

    /**
     * Tokenizes just like WordTokenizer with the exception that "c’h"
     * is not split. "C’h" is considered as a letter in breton (trigraph)
     * and it occurs in many words.  So tokenizer should not split it.
     * Also split things like "n’eo" into 2 tokens only "n’" + "eo".
     *
     * @param text - Text to tokenize
     * @return List of tokens.
     * <p>
     * Note: a special string ##BR_APOS## is used to replace apostrophes
     * during tokenizing.
     */
    @Override
    public List<String> tokenize(final String text) {
        // this is a bit of a hacky way to tokenize.  It should work
        // but I should work on a more elegant way.
        String replaced = text.replaceAll("([Cc])['’‘ʼ]([Hh])", "$1\u0001\u0001BR_APOS\u0001\u0001$2")
                .replaceAll("(\\p{L})['’‘ʼ]", "$1\u0001\u0001BR_APOS\u0001\u0001 ");

        final List<String> tokenList = super.tokenize(replaced);
        List<String> tokens = new ArrayList<>();

        // Put back apostrophes and remove spurious spaces.
        Iterator<String> itr = tokenList.iterator();
        while (itr.hasNext()) {
            String word = itr.next().replace("\u0001\u0001BR_APOS\u0001\u0001", "’");
            tokens.add(word);
            if (!word.equals("’") && word.endsWith("’")) {
                itr.next(); // Skip the next spurious white space.
            }
        }
        return tokens;
    }
}
