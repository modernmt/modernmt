package eu.modernmt.processing.normalizers;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatalanPuntVolatNormalizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private static final Pattern REGEX = Pattern.compile("[Ll]\\s*[·•]\\s*[Ll]|[Ŀŀ]\\s*[Ll]");

    public CatalanPuntVolatNormalizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        if (!Language.CATALAN.getLanguage().equals(sourceLanguage.getLanguage()))
            throw new UnsupportedLanguageException(targetLanguage);
    }

    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) {
        String string = builder.toString();
        SentenceBuilder.Editor editor = builder.edit();

        Matcher matcher = REGEX.matcher(string);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            boolean isUpperCase = Character.isUpperCase(string.charAt(end - 1));
            String replacement = isUpperCase ? "L·L" : "l·l";

            editor.replace(start, end - start, replacement);
        }

        return editor.commit();
    }

}