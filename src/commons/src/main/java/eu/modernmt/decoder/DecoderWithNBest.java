package eu.modernmt.decoder;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.util.UUID;

/**
 * Created by davide on 23/05/17.
 */
public interface DecoderWithNBest {

    Translation translate(UUID user, LanguageDirection direction, Sentence text, int nbestListSize) throws DecoderException;

    Translation translate(UUID user, LanguageDirection direction, Sentence text, ContextVector contextVector, int nbestListSize) throws DecoderException;

}
