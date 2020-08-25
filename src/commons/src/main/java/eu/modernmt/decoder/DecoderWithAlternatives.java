package eu.modernmt.decoder;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Priority;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.util.UUID;

/**
 * Created by davide on 23/05/17.
 */
public interface DecoderWithAlternatives {

    Translation translate(Priority priority, UUID user, LanguageDirection direction, Sentence text, Integer alternatives, long expiration) throws DecoderException;

    Translation translate(Priority priority, UUID user, LanguageDirection direction, Sentence text, ContextVector contextVector, Integer alternatives, long expiration) throws DecoderException;

}
