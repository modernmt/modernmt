package eu.modernmt.processing.numbers;

import eu.modernmt.model.Phrase;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.Arrays;
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
            NumericalPhrase nphrase = new NumericalPhrase(phrase);

            if (nphrase.sourceDigitsCount == 0 && nphrase.targetDigitsCount == 0)
                continue;

            if (nphrase.sourceDigitsCount == nphrase.targetDigitsCount)
                nphrase.copySourceToTargetDigits();
            else
                nphrase.copySourceToTargetWords();
        }

        return translation;
    }

    private static class NumericalPhrase {

        public final Word[] source;
        public final Word[] target;
        public final int[] digitsPerSourceWord;
        public final int[] digitsPerTargetWord;
        public final StringBuilder sourceDigits;
        public final int sourceDigitsCount;
        public final int targetDigitsCount;

        private static int analyze(Word[] words, int[] digitsPerWord, StringBuilder collector, boolean usePlaceholder) {
            int digits = 0;

            for (int w = 0; w < words.length; w++) {
                char[] word = (usePlaceholder ? words[w].getPlaceholder() : words[w].getText()).toCharArray();

                if (collector == null) {
                    for (char c : word) {
                        if (c >= '0' && c <= '9')
                            digitsPerWord[w]++;
                    }
                } else {
                    for (char c : word) {
                        if (c >= '0' && c <= '9') {
                            digitsPerWord[w]++;
                            collector.append(c);
                        }
                    }
                }

                digits += digitsPerWord[w];
            }

            return digits;
        }

        public NumericalPhrase(Phrase phrase) {
            this.source = phrase.getSource();
            this.target = phrase.getTarget();
            this.sourceDigits = new StringBuilder();
            this.digitsPerSourceWord = new int[source.length];
            this.digitsPerTargetWord = new int[target.length];

            this.sourceDigitsCount = analyze(source, digitsPerSourceWord, sourceDigits, false);
            this.targetDigitsCount = analyze(target, digitsPerTargetWord, null, true);
        }

        public void copySourceToTargetDigits() {
            int sourceDigitIndex = 0;

            for (int w = 0; w < target.length && sourceDigitIndex < sourceDigits.length(); w++) {
                if (digitsPerTargetWord[w] == 0)
                    continue;

                char[] text = target[w].getPlaceholder().toCharArray();

                for (int i = 0; i < text.length && sourceDigitIndex < sourceDigits.length(); i++) {
                    if (text[i] >= '0' && text[i] <= '9')
                        text[i] = sourceDigits.charAt(sourceDigitIndex++);
                }

                target[w].setText(new String(text));
            }
        }

        public void copySourceToTargetWords() {
            Word[] sourceWords = getWordsWithDigits(source, digitsPerSourceWord);
            int index = 0;

            for (int w = 0; w < target.length; w++) {
                if (digitsPerTargetWord[w] == 0)
                    continue;

                if (index < sourceWords.length) {
                    target[w].setText(sourceWords[index++].getText());
                } else {
                    char[] text = target[w].getPlaceholder().toCharArray();
                    for (int i = 0; i < text.length; i++) {
                        if (text[i] >= '0' && text[i] <= '9')
                            text[i] = '?';
                    }

                    target[w].setText(new String(text));
                }
            }
        }

        private Word[] getWordsWithDigits(Word[] words, int[] digitsPerWord) {
            int length = 0;
            for (int i = 0; i < words.length; i++) {
                if (digitsPerWord[i] > 0)
                    length++;
            }

            Word[] result = new Word[length];
            int j = 0;

            for (int i = 0; i < words.length; i++) {
                if (digitsPerWord[i] > 0)
                    result[j++] = words[i];
            }

            return result;
        }

        @Override
        public String toString() {
            return "NumericalPhrase{" +
                    "source=" + Arrays.toString(source) +
                    ", target=" + Arrays.toString(target) +
                    ", digitsPerSourceWord=" + Arrays.toString(digitsPerSourceWord) +
                    ", digitsPerTargetWord=" + Arrays.toString(digitsPerTargetWord) +
                    ", sourceDigits=" + sourceDigits +
                    ", sourceDigitsCount=" + sourceDigitsCount +
                    ", targetDigitsCount=" + targetDigitsCount +
                    '}';
        }
    }

}
