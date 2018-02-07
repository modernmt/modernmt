package eu.modernmt.processing.numbers.internal;

import eu.modernmt.model.Word;

public class NumericPlaceholder {

    private final int index;
    private final Word word;
    private final char[] digits;

    public static NumericPlaceholder build(int index, Word word) {
        String text = word.hasText() ? word.getText() : word.getPlaceholder();

        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c >= '0' && c <= '9')
                length++;
        }

        if (length == 0)
            return null;

        char[] digits = new char[length];
        int d = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c >= '0' && c <= '9')
                digits[d++] = c;
        }

        return new NumericPlaceholder(index, word, digits);
    }

    private NumericPlaceholder(int index, Word word, char[] digits) {
        this.index = index;
        this.word = word;
        this.digits = digits;
    }


    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NumericPlaceholder that = (NumericPlaceholder) o;

        return index == that.index;
    }

    @Override
    public int hashCode() {
        return index;
    }

    public char[] getDigits() {
        return digits;
    }

    public Word getWord() {
        return word;
    }

    public int setDigits(char[] digits, int start) {
        System.arraycopy(digits, start, this.digits, 0, this.digits.length);
        String text = NumericUtils.applyDigits(this.digits, this.word.getPlaceholder());

        this.word.setText(text);

        return this.digits.length;
    }

    public void obfuscate() {
        word.setText(NumericUtils.obfuscate(word.getPlaceholder()));
    }

}
