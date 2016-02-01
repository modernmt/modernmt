package eu.modernmt.processing.tokenizer.jflex;

/**
 * Created by davide on 01/02/16.
 */
public class AnnotatedCharArray {

    private char[] chars;

    public AnnotatedCharArray(String string) {
        this(string.toCharArray());
    }

    public AnnotatedCharArray(char[] chars) {
        char[] temp = new char[chars.length];

        boolean start = true;
        boolean whitespace = false;
    }
}
