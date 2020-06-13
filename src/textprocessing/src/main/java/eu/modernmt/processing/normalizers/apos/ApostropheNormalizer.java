package eu.modernmt.processing.normalizers.apos;

import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApostropheNormalizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private static final ConcurrentHashMap<String, Pattern> cache = new ConcurrentHashMap<>();

    private static Pattern load(final Language language) {
        return cache.computeIfAbsent(language.getLanguage(), (key) -> {
            InputStream stream = null;

            try {
                stream = ApostropheNormalizer.class.getResourceAsStream(key + ".txt");
                if (stream == null)
                    throw new UnsupportedLanguageException(language);

                StringBuilder regex = new StringBuilder();
                for (String contraction : IOUtils.readLines(stream, UTF8Charset.get())) {
                    String prefix;
                    if (contraction.charAt(0) != '\'')
                        prefix = "[^\\p{L}]";
                    else
                        prefix = "\\p{L}";

                    String suffix;
                    if (contraction.charAt(contraction.length() - 1) != '\'')
                        suffix = "[^\\p{L}]";
                    else
                        suffix = "\\p{L}";

                    contraction = contraction.toLowerCase();
                    contraction = contraction.replace("'", "\\s*['`‘’]\\s*");

                    regex.append(prefix);
                    regex.append(contraction);
                    regex.append(suffix);
                    regex.append('|');
                }

                return Pattern.compile(regex.substring(0, regex.length() - 1), Pattern.CASE_INSENSITIVE);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        });
    }

    private final Pattern regex;

    public ApostropheNormalizer(Language sourceLanguage, Language targetLanguage) {
        super(sourceLanguage, targetLanguage);
        this.regex = load(sourceLanguage);
    }

    @Override
    public SentenceBuilder call(SentenceBuilder param, Map<String, Object> metadata) throws ProcessingException {
        String string = param.toString();
        Matcher matcher = regex.matcher(' ' + string + ' ');

        SentenceBuilder.Editor editor = param.edit();
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            replace(editor, string, Math.max(0, start - 1), Math.max(0, end - 1));
        }

        return editor.commit();
    }

    private static void replace(SentenceBuilder.Editor editor, String string, int start, int end) {
        // Search for apostrophe
        int index = -1;
        char a = '\0';
        for (int i = start; i < end; i++) {
            char c = string.charAt(i);
            if (c == '\'' || c == '`' || c == '‘' || c == '’') {
                index = i;
                a = c;
                break;
            }
        }

        if (index < 0)
            return;

        // Find left and right edge of replacement
        int right = end;
        for (int i = index + 1; i < end; i++) {
            if (!Character.isWhitespace(string.charAt(i))) {
                right = i;
                break;
            }
        }

        int left = start;
        for (int i = index - 1; i >= start; i--) {
            if (!Character.isWhitespace(string.charAt(i))) {
                left = i + 1;
                break;
            }
        }

        editor.replace(left, right - left, Character.toString(a));
    }

}
