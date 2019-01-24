package eu.modernmt.processing.normalizers;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Created by davide on 12/05/16.
 */
public class ControlCharsRemover extends TextProcessor<String, String> {

    public ControlCharsRemover(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    public static String strip(String string) {
        StringBuilder filtered = new StringBuilder(string.length());

        string.codePoints().filter(
                code -> !(code < 0x09) &&
                        !(0x0d < code && code < 0x20) &&
                        !(0xe000 <= code && code <= 0xf8ff) &&
                        !(0xf0000 <= code && code <= 0xffffd) &&
                        !(0x100000 <= code && code <= 0x10fffd)
        ).forEach(filtered::appendCodePoint);

        return filtered.toString();
    }

    @Override
    public String call(String param, Map<String, Object> metadata) {
        return strip(param);
    }

}
