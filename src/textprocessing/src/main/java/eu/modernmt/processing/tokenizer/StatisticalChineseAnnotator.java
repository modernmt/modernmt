package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashSet;

public class StatisticalChineseAnnotator implements BaseTokenizer.Annotator {

    private final int maxLength;
    private final HashSet<String> words;

    public StatisticalChineseAnnotator() {
        InputStream stream = null;

        try {
            this.words = new HashSet<>(80000);
            int maxLength = 0;

            stream = getClass().getResourceAsStream("chinese-words.list");

            LineIterator lines = IOUtils.lineIterator(stream, Charset.forName("UTF-8"));
            while (lines.hasNext()) {
                String line = lines.nextLine();

                this.words.add(line);
                maxLength = Math.max(maxLength, line.length());
            }

            this.maxLength = maxLength;
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @Override
    public void annotate(TokenizedString string) throws ProcessingException {
        String text = string.toString();

        int i = 0;

        while (i < text.length() - 1) {
            int length;

            for (length = maxLength; length > 1; length--) {
                if (i + length > text.length())
                    continue;

                String word = text.substring(i, i + length);
                if (words.contains(word)) {
                    string.protect(i + 1, i + length);
                    break;
                }
            }

            i += length;
        }
    }

}
