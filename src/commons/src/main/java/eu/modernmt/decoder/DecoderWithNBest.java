package eu.modernmt.decoder;

import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

/**
 * Created by davide on 23/05/17.
 */
public interface DecoderWithNBest extends Decoder {

    Translation translate(Sentence text, int nbestListSize) throws DecoderException;

    Translation translate(Sentence text, ContextVector contextVector, int nbestListSize) throws DecoderException;

}
