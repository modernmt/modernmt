package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;

public class StatisticalChineseAnnotator implements BaseTokenizer.Annotator {

    private static class Dictionary {

        private static Dictionary load(String filename) throws IOException {
            InputStream stream = null;

            try {
                HashSet<String> words = new HashSet<>(21000);
                int maxLength = 0;

                stream = StatisticalChineseAnnotator.class.getResourceAsStream(filename);

                LineIterator lines = IOUtils.lineIterator(stream, Charset.forName("UTF-8"));
                while (lines.hasNext()) {
                    String line = lines.nextLine();

                    words.add(line);
                    maxLength = Math.max(maxLength, line.length());
                }

                return new Dictionary(maxLength, words);
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }

        private final int maxLength;
        private final HashSet<String> words;

        private Dictionary(int maxLength, HashSet<String> words) {
            this.maxLength = maxLength;
            this.words = words;
        }

        public boolean contains(String word) {
            return words.contains(word);
        }

        public int getWordMaxLength() {
            return maxLength;
        }
    }

    private static Dictionary dictionary = null;

    private static Dictionary getDictionary() throws ProcessingException {
        if (dictionary == null) {
            synchronized (StatisticalChineseAnnotator.class) {
                if (dictionary == null) {
                    try {
                        dictionary = Dictionary.load("chinese-words.list");
                    } catch (IOException e) {
                        throw new ProcessingException("Failed to load Chinese dictionary: chinese-words.list");
                    }
                }
            }
        }

        return dictionary;
    }

    @Override
    public void annotate(TokenizedString string) throws ProcessingException {
        Dictionary dictionary = getDictionary();
        String text = string.toString();

        int i = 0;

        while (i < text.length() - 1) {
            int length;

            for (length = dictionary.getWordMaxLength(); length > 1; length--) {
                if (i + length > text.length())
                    continue;

                String word = text.substring(i, i + length);
                if (dictionary.contains(word)) {
                    string.protect(i + 1, i + length);
                    break;
                }
            }

            i += length;
        }
    }

}
