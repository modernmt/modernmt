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

    /**
     * This method states whether this decoder supports the sentence splittin feature or not.
     * The sentence split feature checks if a passed sentence is actually a period witH multiple separate sentences;
     * if it is, a decoder supporting the sentence split feature will handle each sentence separately.
     * @return TRUE if this decoder supports the Sentence Split feature, FALSE otherwise
     */
    boolean supportsSentenceSplit();
}
