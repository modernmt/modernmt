package eu.modernmt.processing.numbers;

import eu.modernmt.model.Word;
import eu.modernmt.processing.SentenceBuilder;
import eu.modernmt.processing.WordTransformationFactory;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by davide on 04/04/16.
 */
public class NumericWordFactory implements SentenceBuilder.WordFactory, WordTransformationFactory.WordTransformer {

    private static final Pattern PATTERN = Pattern.compile(".*[0-9].*");
    private static final NumericWordTransformation TRANSFORMATION = new NumericWordTransformation();

    private static class Counter {
        public int value;

        Counter(int value) {
            this.value = value;
        }
    }

    private HashMap<String, Counter> pattern2Count = new HashMap<>();

    // WordTransformationFactory.WordTransformer

    @Override
    public boolean match(Word word) {
        return PATTERN.matcher(word.getPlaceholder()).find();
    }

    @Override
    public Word setupTransformation(Word word) {
        word.setTransformation(TRANSFORMATION);
        return word;
    }

    // SentenceBuilder.WordFactory

    @Override
    public boolean match(String text, String placeholder) {
        return PATTERN.matcher(text).find();
    }

    @Override
    public Word build(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        char[] chars = text.toCharArray();

        replaceDigits(chars, 0);
        placeholder = new String(chars);

        Counter count = pattern2Count.get(placeholder);
        if (count == null) {
            pattern2Count.put(placeholder, new Counter(1));
        } else {
            replaceDigits(chars, count.value);
            placeholder = new String(chars);

            count.value++;
        }

        Word word = new Word(text, placeholder, rightSpace, rightSpaceRequired);
        word.setTransformation(TRANSFORMATION);

        return word;
    }

    private static void replaceDigits(char[] chars, int digit) {
        char d = (char) ('0' + (digit % 10));

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= '0' && c <= '9')
                chars[i] = d;
        }
    }

    private static class NumericWordTransformation implements Word.Transformation {

        @Override
        public void apply(Word source, Word target) {
            if (source == null) {
                // Default transformation does nothing.
                return;
            }

            int sourceDigits = countDigits(source.getPlaceholder().toCharArray());
            if (sourceDigits == 0) {
                // If the source is not a Numeric token, skip transformation.
                return;
            }

            char[] output = target.getPlaceholder().toCharArray();

            if (countDigits(output) == sourceDigits) {
                char[] sourceText = source.getText().toCharArray();

                int sourceIndex = 0;
                for (int i = 0; i < output.length; i++) {
                    char p = output[i];

                    if (p >= '0' && p <= '9') {
                        for (; sourceIndex < sourceText.length; sourceIndex++) {
                            char s = sourceText[sourceIndex];

                            if (s >= '0' && s <= '9') {
                                output[i] = s;
                                sourceIndex++;
                                break;
                            }
                        }
                    }
                }

                target.setText(new String(output));
            } else {
                target.setText(source.getText());
            }
        }

        private static int countDigits(char[] chars) {
            int counter = 0;
            for (char c : chars) {
                if (c >= '0' && c <= '9')
                    counter++;
            }

            return counter;
        }

    }
}
