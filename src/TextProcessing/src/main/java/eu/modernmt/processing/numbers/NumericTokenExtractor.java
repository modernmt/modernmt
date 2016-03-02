package eu.modernmt.processing.numbers;

import eu.modernmt.model.PlaceholderToken;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;
import eu.modernmt.processing.framework.TextProcessor;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by davide on 02/03/16.
 */
public class NumericTokenExtractor<T extends Sentence> implements TextProcessor<T, T> {

    private static class Counter {
        public int value;

        public Counter(int value) {
            this.value = value;
        }
    }

    @Override
    public T call(T sentence) {
        HashMap<String, Counter> pattern2Count = new HashMap<>();

        Token[] tokens = sentence.getTokens();

        for (int i = 0; i < tokens.length; i++) {
            Token token = tokens[i];

            if (token instanceof PlaceholderToken)
                continue;

            String originalText = token.getText();
            char[] text = originalText.toCharArray();

            if (!replaceDigits(text, 0))
                continue;

            String placeholder = new String(text);

            Counter count = pattern2Count.get(placeholder);
            if (count == null) {
                pattern2Count.put(placeholder, new Counter(1));
            } else {
                replaceDigits(text, count.value);
                placeholder = new String(text);

                count.value++;
            }

            tokens[i] = new NumericToken(originalText, placeholder, token.hasRightSpace());
        }

        return sentence;
    }

    private static boolean replaceDigits(char[] chars, int digit) {
        char d = (char) ('0' + (digit % 10));
        boolean replaced = false;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= '0' && c <= '9') {
                replaced = true;
                chars[i] = d;
            }
        }

        return replaced;
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }
}
