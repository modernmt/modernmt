package eu.modernmt.decoder;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.TranslationMemory;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Priority;
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

    public abstract TranslationMemory getTranslationMemory();

    public abstract void setListener(DecoderListener listener);

    public abstract boolean isLanguageSupported(LanguageDirection language);

    public abstract Translation translate(Priority priority, UUID user, LanguageDirection direction, Sentence text, long expiration
    	,String[] s,String[] t,String[] w) throws DecoderException;

    public abstract Translation translate(Priority priority, UUID user, LanguageDirection direction, Sentence text, ContextVector contextVector, long expiration
    	,String[] s,String[] t,String[] w) throws DecoderException;

    public abstract void test() throws DecoderException;



}
