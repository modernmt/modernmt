package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;

import static org.junit.Assert.fail;

/**
 * Created by davide on 22/03/16.
 */
public class Assertions {

    public static void assertCoherentSpacing(Sentence sentence) {
        int j = 0;

        Token[] tokens = new Token[sentence.length()];
        for (Token token : sentence)
            tokens[j++] = token;

        for (int i = 0; i < tokens.length; i++) {
            Token token = tokens[i];
            if (!(token instanceof Tag))
                continue;
            Tag tag = (Tag) token;

            if (i == 0) {
                if (tag.hasLeftSpace())
                    fail("Starting XML tag has left space");
            } else {
                Token previous = tokens[i - 1];
                if (tag.hasLeftSpace() != previous.hasRightSpace())
                    fail("XML Tag at position " + i + " has inconsistent left space information: expected " + previous.hasRightSpace() + ", found " + tag.hasLeftSpace());
            }
        }
    }

}
