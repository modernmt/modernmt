package eu.modernmt.processing;

import java.util.BitSet;

/**
 * Created by davide on 19/02/16.
 */
public class AnnotatedString {

    public final String string;
    public final BitSet bits;

    public AnnotatedString(String string) {
        this.string = string;
        this.bits = new BitSet(string.length());
    }

    public AnnotatedString(String string, BitSet bits) {
        this.string = string;
        this.bits = bits;
    }

}
