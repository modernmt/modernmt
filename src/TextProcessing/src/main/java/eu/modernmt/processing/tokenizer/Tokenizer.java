package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.XMLEditableString;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 26/01/16.
 */
public abstract class Tokenizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    public static final String KEY_ENABLE = "Tokenizer.ENABLE";

    public Tokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public final XMLEditableString call(XMLEditableString param, Map<String, Object> metadata) throws ProcessingException {
        Boolean enable = (Boolean) metadata.get(KEY_ENABLE);

        return (enable == null || enable) ? tokenize(param, metadata) : WhitespaceTokenizer.tokenize(param);
    }

    protected abstract XMLEditableString tokenize(XMLEditableString param, Map<String, Object> metadata) throws ProcessingException;

}
