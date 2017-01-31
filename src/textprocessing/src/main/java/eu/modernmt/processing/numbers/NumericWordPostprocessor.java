package eu.modernmt.processing.numbers;

import eu.modernmt.model.Phrase;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 08/04/16.
 */
public class NumericWordPostprocessor extends TextProcessor<Translation, Translation> {

    public NumericWordPostprocessor(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        for (Phrase phrase : translation.getPhrases()) {
            Word[] source = phrase.getSource();
            Word[] target = phrase.getTarget();

            StringBuilder sourceDigits = new StringBuilder();
            int sourceWordsWithNumbers = extractDigits(source, sourceDigits);

            if (sourceWordsWithNumbers == 0)
                continue;

            int[] counts = count(target);
            int targetDigits = counts[0];
            
        }

        return translation;
    }

    private int extractDigits(Word[] words, StringBuilder output) {
        int wordCount = 0;
        for (Word word : words) {
            boolean hasDigit = false;

            for (char c : word.getPlaceholder().toCharArray()) {
                if (c >= '0' && c <= '9') {
                    hasDigit = true;
                    output.append(c);
                }
            }

            if (hasDigit)
                wordCount++;
        }

        return wordCount;
    }

    private int[] count(Word[] words) {
        int digits = 0;
        int wordCount = 0;

        for (Word word : words) {
            boolean hasDigit = false;

            for (char c : word.getPlaceholder().toCharArray()) {
                if (c >= '0' && c <= '9') {
                    hasDigit = true;
                    digits++;
                }
            }

            if (hasDigit)
                wordCount++;
        }

        return new int[]{digits, wordCount};
    }

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
