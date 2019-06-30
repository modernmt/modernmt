package eu.modernmt.processing.tokenizer.abbr;

import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.lang.Language2;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AbbreviationAnnotator implements BaseTokenizer.Annotator {

    private static final ConcurrentHashMap<String, AbbreviationAnnotator> instances = new ConcurrentHashMap<>(64);

    public static AbbreviationAnnotator getInstance(Language2 language) {
        return getInstance(language, false);
    }

    public static AbbreviationAnnotator getInstance(Language2 language, boolean caseless) {
        return instances.computeIfAbsent(language.getLanguage(), lang -> {
            try {
                return new AbbreviationAnnotator(lang + ".txt", caseless);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        });
    }

    private static HashMap<String, Boolean> readResource(String resourceName, boolean caseless) throws IOException {
        UnixLineReader reader = null;

        try {
            InputStream stream = AbbreviationAnnotator.class.getResourceAsStream(resourceName);
            if (stream == null)
                throw new IOException("Abbreviation resource not found: " + resourceName);

            reader = new UnixLineReader(stream, UTF8Charset.get());

            HashMap<String, Boolean> words = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.charAt(0) == '#')
                    continue;

                Boolean value;

                if (line.endsWith("#NUMERIC_ONLY#")) {
                    line = line.replace("#NUMERIC_ONLY#", "").trim();
                    value = Boolean.TRUE;
                } else {
                    value = Boolean.FALSE;
                }

                if (line.length() < 2 || line.charAt(line.length() - 1) != '.')
                    throw new IOException("Invalid word in abbreviations file: " + line);

                if (caseless && line.length() > 2)  // 1 char + full-stop
                    line = line.toLowerCase();

                Boolean oldValue = words.get(line);
                if (oldValue != null && oldValue != value)
                    throw new IOException("Inconsistent word rule in abbreviations file: " + line);

                words.put(line, value);
            }

            if (words.isEmpty())
                throw new IOException("File does not contain valid abbreviations");

            return words;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private static char[] getAlphabet(Set<String> words, boolean caseless) {
        HashSet<Character> set = new HashSet<>();
        for (String word : words) {
            if (caseless)
                word = word.toLowerCase();

            for (int i = 0; i < word.length(); i++) {
                set.add(word.charAt(i));
            }
        }

        int i = 0;
        char[] alphabet = new char[set.size()];
        for (char c : set)
            alphabet[i++] = c;

        return alphabet;
    }

    private int getMaxLength(Set<String> words) {
        int len = 0;
        for (String word : words) {
            if (word.length() > len)
                len = word.length();
        }

        return len;
    }

    private static final char[] SEPARATORS = " !¡\\#$%&\"'*+,-./:;<=>?¿@[]^_`{|}~()".toCharArray();
    private final HashMap<String, Boolean> words;
    private final char[] alphabet;
    private final boolean caseless;
    private final int maxLength;

    private AbbreviationAnnotator(String resourceName, boolean caseless) throws IOException {
        this.words = readResource(resourceName, caseless);
        this.alphabet = getAlphabet(words.keySet(), caseless);
        this.maxLength = getMaxLength(words.keySet());
        this.caseless = caseless;
    }

    private boolean contains(char[] chars, char c) {
        if (this.caseless)
            c = Character.toLowerCase(c);

        for (char o : chars) {
            if (o == c)
                return true;
        }

        return false;
    }

    private int getLongestMatch(String chars, int end) {
        int result = -1;
        boolean digitFollows = end + 2 < chars.length()
                && chars.charAt(end + 1) == ' '
                && Character.isDigit(chars.charAt(end + 2));

        int left = Math.max(0, end - maxLength);

        for (int i = end - 1; i >= left; i--) {
            char c = chars.charAt(i);
            boolean acceptable = contains(this.alphabet, c);

            if (contains(SEPARATORS, c)) {
                String word = chars.substring(i + 1, end + 1);

                if (caseless && word.length() > 2)  // 1 char + full-stop
                    word = word.toLowerCase();

                Boolean numberOnly = this.words.get(word);
                if (numberOnly != null && (!numberOnly || digitFollows))
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
