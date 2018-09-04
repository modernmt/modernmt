package eu.modernmt.processing.normalizers;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 11/12/17.
 */
public class ItalianAccentMarkProcessor extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private final static Pattern COMMON = Pattern.compile("(((^|\\W)e)|\\w+[a-z][r][ao]|\\w+[gnr]no|\\w+io|\\w+t[aeiou]|(cio|cosi|gia|laggiu|lassu|li|la|menu|piu|puo|pero|quaggiu|quassu|si|lunedi|martedi|mercoledi|giovedi|venerdi))'", Pattern.CASE_INSENSITIVE);
    private final static Pattern SPECIAL_WORDS = Pattern.compile(
            "(^|\\W)(affinch|alch|alcunch|allorch|almenoch|altroch|amenoch|ammenoch|ancorch|anzicch|anzich|" +
                    "bench|ch|checch|cosicch|dopodich|finch|fintantoch|fuorch|giacch|granch|macch|nonch|perch|poich" +
                    "|pressocch|pressoch|semprech|sennonch|senonch|sicch|n|\\w+tr)(e'|è)", Pattern.CASE_INSENSITIVE);

    public ItalianAccentMarkProcessor(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        if (!Language.ITALIAN.getLanguage().equals(sourceLanguage.getLanguage()))
            throw new UnsupportedLanguageException(sourceLanguage, targetLanguage);
    }

    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) throws ProcessingException {
        builder = replaceSpecialWords(builder);
        builder = replaceCommonWords(builder);

        return builder;
    }

    private static SentenceBuilder replaceSpecialWords(SentenceBuilder builder) {
        String string = builder.toString();
        SentenceBuilder.Editor editor = builder.edit();

        Matcher matcher = SPECIAL_WORDS.matcher(string);
        while (matcher.find()) {
            int end = matcher.end();

            int begin = string.charAt(end - 1) == '\'' ? end - 2 : end - 1;
            boolean isUpperCase = Character.isUpperCase(string.charAt(begin));

            editor.replace(begin, end - begin, isUpperCase ? "É" : "é");
        }

        return editor.commit();
    }

    private static SentenceBuilder replaceCommonWords(SentenceBuilder builder) {
        // Find isolated apostrophes
        char[] array = builder.toCharArray();

        int pendingApostrophesCount = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == '\'') {
//                boolean leftWhitespace = (i == 0 || array[i - 1] == ' ');
//                boolean rightWhitespace = (i == array.length - 1 || array[i + 1] == ' ');
                //TODO: fix for "citta'."
                boolean leftWhitespace = (i == 0 || ! Character.isLetter(array[i - 1]) );
                boolean rightWhitespace = (i == array.length - 1 || ! Character.isLetter(array[i + 1]));

                if (leftWhitespace) {
                    if (rightWhitespace) {
                        if (pendingApostrophesCount > 0)
                            pendingApostrophesCount--;


                        array[i] = ' ';  // cannot be an accent mark
                    } else {
                        pendingApostrophesCount++;
                        array[i] = ' ';  // cannot be an accent mark
                    }
                } else {
                    if (rightWhitespace) {
                        if (pendingApostrophesCount > 0) {
                            pendingApostrophesCount--;
                            array[i] = ' ';  // cannot be an accent mark
                        }
                    } else {
                        array[i] = ' ';  // cannot be an accent mark (l'esempio)
                    }
                }
            }
        }

        String string = new String(array);
        SentenceBuilder.Editor editor = builder.edit();

        Matcher matcher = COMMON.matcher(string);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

//            if (end - start == 4 && "are'".equals(matcher.group()))
//                continue;

            String replacement;
            switch (Character.toLowerCase(array[end - 2])) {
                case 'a':
                    replacement = "à";
                    break;
                case 'e':
                    replacement = "è";
                    break;
                case 'i':
                    replacement = "ì";
                    break;
                case 'o':
                    replacement = "ò";
                    break;
                case 'u':
                    replacement = "ù";
                    break;
                default:
                    throw new Error("Should never happen");
            }

            if (Character.isUpperCase(array[end - 2]))
                replacement = replacement.toUpperCase(Locale.ITALIAN);

            editor.replace(end - 2, 2, replacement);
        }

        return editor.commit();
    }

}
