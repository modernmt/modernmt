package eu.modernmt.processing.transliteration;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;

public class AlphabetNormalizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    public AlphabetNormalizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        if (!Language.SERBIAN.isEqualOrMoreGenericThan(sourceLanguage))
            throw new UnsupportedLanguageException(sourceLanguage);
    }

    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) {
        String text = builder.toString();
        String edited = Serbian.toLatin(text);

        if (!text.equals(edited)) {
            SentenceBuilder.Editor editor = builder.edit();
            editor.replace(0, text.length(), edited);
            editor.commit();
        }

        return builder;
    }

}
