package eu.modernmt.processing.recaser;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 03/03/16.
 */
public class UpperCasePreprocessor extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    public static final String ANNOTATION = UpperCasePreprocessor.class.getCanonicalName();

    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) throws ProcessingException {
        char[] chars = builder.toCharArray();

        if (isUpperCase(chars)) {
            normalizeCase(chars);
            handleDNT(chars);

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

    private static void handleDNT(char[] chars) {
        // String to be scanned to find the pattern.
        String pattern = "\\$\\{DNT(\\d)\\}";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(new String(chars));
        while (m.find()) {
            int start = m.start() + 2;
            chars[start] = 'D';
            chars[start + 1] = 'N';
            chars[start + 2] = 'T';
        }
    }

}
