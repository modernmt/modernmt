package eu.modernmt.decoder;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.io.Closeable;
import java.io.File;

/**
 * Created by davide on 26/11/15.
 */
public abstract class Decoder implements Closeable {

    public Decoder(File model, DecoderConfig config) throws DecoderException {
        // no-op
    }

    public abstract void setListener(DecoderListener listener);

    public abstract Translation translate(LanguagePair direction, Sentence text) throws DecoderException;

    public abstract Translation translate(LanguagePair direction, Sentence text, ContextVector contextVector) throws DecoderException;

    /**
     * This method states whether this decoder supports the sentence splitting feature or not.
     * The sentence split feature checks if a passed sentence is actually a period with multiple separate sentences;
     * if it is, a decoder supporting the sentence split feature will handle each sentence separately.
     *
     * @return TRUE if this decoder supports the Sentence Split feature, FALSE otherwise
     */
    public abstract boolean supportsSentenceSplit();
}
