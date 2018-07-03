package eu.modernmt.decoder;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.io.Closeable;
import java.io.File;
import java.util.UUID;

/**
 * Created by davide on 26/11/15.
 */
public abstract class Decoder implements Closeable {

    public Decoder(File model, DecoderConfig config) throws DecoderException {
        // no-op
    }

    public abstract void setListener(DecoderListener listener);

    public abstract Translation translate(UUID user, LanguagePair direction, Sentence text) throws DecoderException;

    public abstract Translation translate(UUID user, LanguagePair direction, Sentence text, ContextVector contextVector) throws DecoderException;

}
