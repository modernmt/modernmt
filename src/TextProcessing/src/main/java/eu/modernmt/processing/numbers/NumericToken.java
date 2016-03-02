package eu.modernmt.processing.numbers;

import eu.modernmt.model.PlaceholderToken;

/**
 * Created by davide on 02/03/16.
 */
public class NumericToken extends PlaceholderToken {

    public NumericToken(String text, String placeholder) {
        super(text, placeholder);
    }

    public NumericToken(String text, String placeholder, boolean rightSpace) {
        super(text, placeholder, rightSpace);
    }

    public NumericToken(String placeholder) {
        super(placeholder);
    }

    public NumericToken(String placeholder, boolean rightSpace) {
        super(placeholder, rightSpace);
    }

    @Override
    public void applyTransformation(PlaceholderToken sourceToken) {
        char[] placeholder = this.placeholder.toCharArray();

        if (countDigits(placeholder) == countDigits(sourceToken.getPlaceholder().toCharArray())) {
            char[] source = sourceToken.getText().toCharArray();

            int sourceIndex = 0;
            for (int i = 0; i < placeholder.length; i++) {
                char p = placeholder[i];

                if (p >= '0' && p <= '9') {
                    for (; sourceIndex < source.length; sourceIndex++) {
                        char s = source[sourceIndex];

                        if (s >= '0' && s <= '9') {
                            placeholder[i] = s;
                            sourceIndex++;
                            break;
                        }
                    }
                }
            }

            this.text = new String(placeholder);
        } else {
            this.text = sourceToken.getText();
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
