package eu.modernmt.processing.tokenizer.kuromoji;

import com.atilika.kuromoji.ipadic.Token;
import eu.modernmt.model.Languages;
import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.XMLEditableString;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 11/11/15.
 */
public class KuromojiTokenizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    private com.atilika.kuromoji.ipadic.Tokenizer tokenizer = new com.atilika.kuromoji.ipadic.Tokenizer();

    public KuromojiTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
        if (!Languages.sameLanguage(Languages.JAPANESE, sourceLanguage))
            throw new LanguageNotSupportedException(sourceLanguage);
    }

    @Override
    public XMLEditableString call(XMLEditableString text, Map<String, Object> metadata) throws ProcessingException {
        List<Token> tokens = tokenizer.tokenize(text.toString());
        String[] array = new String[tokens.size()];

        for (int i = 0; i < array.length; i++)
            array[i] = tokens.get(i).getSurface();

        return TokenizerOutputTransformer.transform(text, array);
    }

}
