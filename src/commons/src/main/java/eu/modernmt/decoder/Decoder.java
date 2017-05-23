package eu.modernmt.decoder;

import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.io.Closeable;

/**
 * Created by davide on 26/11/15.
 */
public interface Decoder extends Closeable {

    Translation translate(Sentence text) throws DecoderException;

    Translation translate(Sentence text, ContextVector contextVector) throws DecoderException;

}
