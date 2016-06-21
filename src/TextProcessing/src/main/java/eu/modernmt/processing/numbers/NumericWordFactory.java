package eu.modernmt.processing.numbers;

import eu.modernmt.model.Word;
import eu.modernmt.processing.SentenceBuilder;
import eu.modernmt.processing.WordTransformationFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by davide on 04/04/16.
 */
public class NumericWordFactory implements SentenceBuilder.WordFactory, WordTransformationFactory.WordTransformer {

    private static final Pattern PATTERN = Pattern.compile(".*[0-9].*");
    private static final NumericWordTransformation TRANSFORMATION = new NumericWordTransformation();

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
    public Word build(String text, String placeholder, String rightSpace) {
        char[] chars = placeholder.toCharArray();

        replaceDigits(chars, 0);

        Word word = new Word(text, new String(chars), rightSpace);
        word.setTransformation(TRANSFORMATION);

        return word;
    }

    @Override
    public void setMetadata(Map<String, Object> metadata) {
        // Nothing to do
    }

    private static void replaceDigits(char[] chars, int digit) {
        char d = (char) ('0' + (digit % 10));

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= '0' && c <= '9')
                chars[i] = d;
        }
    }

}
