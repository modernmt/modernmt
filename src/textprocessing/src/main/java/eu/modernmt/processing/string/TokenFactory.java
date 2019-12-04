package eu.modernmt.processing.string;

import eu.modernmt.model.*;

/**
 * Created by andrea on 22/02/17.
 * <p>
 * A TokenFactory is an object that is able to build Tokens.
 * Each type of Token requires a specific implementation of TokenFactory.
 */
public interface TokenFactory {

    /**
     * A WORD_FACTORY is an implementation of Token Factory that creates Words
     */
    TokenFactory WORD_FACTORY = new TokenFactory() {
        @Override
        public Token build(String text, String placeholder, boolean hasLeftSpace, String rightSpace, int position) {
            return new Word(text, placeholder, rightSpace);
        }

        @Override
        public String toString() {
            return "Word Factory";
        }
    };

    /**
     * Method that builds a Token object
     *
     * @param hasLeftSpace boolean that says if there is a space between the new Token and the previous one
     * @param rightSpace   String conveying the space between the new Token and the next one
     * @param text         String with the text target of the Transformation for this Token
     * @param position     int conveying the amount of WORD tokens already created (it is meaningful only if the token to create is a TAG
     * @return the newly created Token
     */
    Token build(String text, String placeholder, boolean hasLeftSpace, String rightSpace, int position);

}
