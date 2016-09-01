package eu.modernmt.processing.numbers;

import eu.modernmt.model.Word;

/**
 * Created by davide on 08/04/16.
 */
class NumericWordTransformation implements Word.Transformation {

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
