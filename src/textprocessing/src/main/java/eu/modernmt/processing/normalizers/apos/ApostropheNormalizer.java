package eu.modernmt.processing.normalizers.apos;

import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApostropheNormalizer extends TextProcessor<String, String> {

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
    public String call(String string, Map<String, Object> metadata) throws ProcessingException {
        String placeholder = ' ' + string + ' ';

        BitSet bits = new BitSet(placeholder.length());
        Matcher matcher = regex.matcher(placeholder);

        while (matcher.find())
            annotate(placeholder, bits, matcher.start(), matcher.end());

        return bits.isEmpty() ? string : compile(bits, placeholder);
    }

    private static void annotate(String string, BitSet bits, int start, int end) {
        for (int i = start; i < end; i++) {
            switch (string.charAt(i)) {
                case '\'':
                case '`':
                case '‘':
                case '’':
                    for (int j = i + 1; j < end; j++) {
                        if (Character.isWhitespace(string.charAt(j)))
                            bits.set(j);
                        else
                            break;
                    }

                    for (int j = i - 1; j >= start; j--) {
                        if (Character.isWhitespace(string.charAt(j)))
                            bits.set(j);
                        else
                            break;
                    }

                    break;
            }
        }
    }

    private String compile(BitSet bits, String string) {
        StringBuilder result = new StringBuilder(string.length());
        for (int i = 1; i < string.length() - 1; i++) {
            if (!bits.get(i))
                result.append(string.charAt(i));
        }
        return result.toString();
    }

}
