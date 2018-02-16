package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.string.SentenceBuilder;

/**
 * Created by davide on 03/08/17.
 */
public class ChineseJFlexTokenizer extends JFlexTokenizer {

    public ChineseJFlexTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
        System.out.println("sono qui");

        if (!Language.CHINESE.getLanguage().equals(sourceLanguage.getLanguage()))
            throw new UnsupportedLanguageException(sourceLanguage, targetLanguage);
    }

    @Override
    protected TokensAnnotatedString wrap(SentenceBuilder builder) {
        return new TokensAnnotatedString(builder.toString(), true);
    }
}
