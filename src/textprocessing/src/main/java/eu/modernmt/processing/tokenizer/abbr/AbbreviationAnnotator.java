package eu.modernmt.processing.tokenizer.abbr;

import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class AbbreviationAnnotator implements BaseTokenizer.Annotator {

    private static final ConcurrentHashMap<String, AbbreviationAnnotator> instances = new ConcurrentHashMap<>(64);

    public static AbbreviationAnnotator getInstance(Language language) {
        return instances.computeIfAbsent(language.getLanguage(), lang -> {
            try {
                return new AbbreviationAnnotator(lang + ".txt");
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        });
    }

    private static HashSet<String> readResource(String resourceName) throws IOException {
        UnixLineReader reader = null;

        try {
            InputStream stream = AbbreviationAnnotator.class.getResourceAsStream(resourceName);
            if (stream == null)
                throw new IOException("Abbreviation resource not found: " + resourceName);

            reader = new UnixLineReader(stream, UTF8Charset.get());

            HashSet<String> words = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.charAt(0) == '#')
                    continue;

                words.add(line);
            }

            if (words.isEmpty())
                throw new IOException("File does not contain valid abbreviations");

            return words;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private char[] getAlphabet(HashSet<String> words) {
        HashSet<Character> set = new HashSet<>();
        for (String word : words) {
            for (int i = 0; i < word.length(); i++)
                set.add(word.charAt(i));
        }

        int i = 0;
        char[] alphabet = new char[set.size()];
        for (char c : set)
            alphabet[i++] = c;

        return alphabet;
    }

    private static final char[] SEPARATORS = " !¡\\#$%&\"'*+,-./:;<=>?¿@[]^_`{|}~()".toCharArray();
    private final HashSet<String> words;
    private final char[] alphabet;

    private AbbreviationAnnotator(String resourceName) throws IOException {
        this.words = readResource(resourceName);
        this.alphabet = getAlphabet(words);
    }

    private static boolean contains(char[] chars, char c) {
        for (char o : chars) {
            if (o == c)
                return true;
        }

        return false;
    }

    private int getLongestMatch(String chars, int end) {
        int result = -1;

        for (int i = end - 1; i >= 0; i--) {
            char c = chars.charAt(i);
            boolean acceptable = contains(this.alphabet, c);

            if (contains(SEPARATORS, c)) {
                String word = chars.substring(i + 1, end + 1);
                if (this.words.contains(word))
                    result = i + 1;
            }

            if (!acceptable)
                break;
        }

        return result;
    }

    @Override
    public void annotate(TokenizedString string) {
        String chars = string.toString();

        for (int i = chars.length() - 1; i >= 0; i--) {
            if (chars.charAt(i) == '.') {
                int left = getLongestMatch(chars, i);

                if (left >= 0) {
                    string.protect(left + 1, i + 1);
                    i = left;
                }
            }
        }
    }

}
