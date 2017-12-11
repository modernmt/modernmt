package eu.modernmt.processing.recaser;

import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 03/03/16.
 */
public class UpperCasePreprocessor extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    public static final String ANNOTATION = UpperCasePreprocessor.class.getCanonicalName();

    public UpperCasePreprocessor(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) throws ProcessingException {
        char[] chars = builder.toCharArray();

        if (isUpperCase(chars)) {
            normalizeCase(chars);
            builder.addAnnotation(ANNOTATION);

            SentenceBuilder.Editor editor = builder.edit();
            editor.replace(0, chars.length, new String(chars));
            return editor.commit();
        } else {
            return builder;
        }
    }

    private static boolean isUpperCase(char[] chars) {
        boolean upperCaseFound = false;

        for (int i = 0; i < chars.length; i++) {
            if (!upperCaseFound && Character.isUpperCase(chars[i]))
                upperCaseFound = true;

            if (Character.isLowerCase(chars[i]))
                return false;
        }

        return upperCaseFound;
    }

    private static void normalizeCase(char[] chars) {
        boolean firstLetter = false;

        for (int i = 0; i < chars.length; i++) {
            if (Character.isUpperCase(chars[i])) {
                if (!firstLetter) {
                    firstLetter = true;
                } else {
                    chars[i] = Character.toLowerCase(chars[i]);
                }

            }
        }
    }

}
