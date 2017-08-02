package eu.modernmt.decoder;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.io.Closeable;

/**
 * Created by davide on 26/11/15.
 */
public interface Decoder extends Closeable {

    void setListener(DecoderListener listener);

    Translation translate(LanguagePair direction, Sentence text) throws DecoderException;

    Translation translate(LanguagePair direction, Sentence text, ContextVector contextVector) throws DecoderException;

}
