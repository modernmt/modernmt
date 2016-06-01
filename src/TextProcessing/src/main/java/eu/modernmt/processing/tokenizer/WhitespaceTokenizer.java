package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.XMLEditableString;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 01/06/16.
 */
public final class WhitespaceTokenizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    public WhitespaceTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public XMLEditableString call(XMLEditableString param, Map<String, Object> metadata) throws ProcessingException {
        return tokenize(param);
    }

    public static XMLEditableString tokenize(XMLEditableString param) throws ProcessingException {
        String[] tokens = param.toString().trim().split(" +");

        if (tokens.length == 1 && tokens[0].isEmpty())
            return TokenizerOutputTransformer.transform(param, new String[0]);
        else
            return TokenizerOutputTransformer.transform(param, tokens);
    }

}

