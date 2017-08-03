package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.lang.Languages;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.Locale;

/**
 * Created by davide on 03/08/17.
 */
public class ChineseJFlexTokenizer extends JFlexTokenizer {

    public ChineseJFlexTokenizer(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        if (!Languages.sameLanguage(Locale.CHINESE, sourceLanguage))
            throw new UnsupportedLanguageException(sourceLanguage, targetLanguage);
    }

    @Override
    protected TokensAnnotatedString wrap(SentenceBuilder builder) {
        return new TokensAnnotatedString(builder.toString(), true);
    }
}
