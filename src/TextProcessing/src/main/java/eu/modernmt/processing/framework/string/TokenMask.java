package eu.modernmt.processing.framework.string;

import java.util.BitSet;
import java.util.Iterator;

/**
 * Created by davide on 08/04/16.
 */
class TokenMask implements Iterable<int[]> {

    private final int length;
    private final BitSet leftSplit;
    private final BitSet rightSplit;

    public TokenMask(int length) {
        this.length = length;
        this.leftSplit = new BitSet(length);
        this.rightSplit = new BitSet(length);
    }

    public void setToken(int start, int length) {
        int end = start + length;

        this.leftSplit.set(start);
        if (length > 1)
            this.leftSplit.clear(start + 1, end);

        this.rightSplit.set(end - 1);
        if (length > 1)
            this.rightSplit.clear(start, end - 1);
    }

    @Override
    public Iterator<int[]> iterator() {
        return new Iterator<int[]>() {

            private boolean computed = false;
            private final int[] next = new int[2];

            private int i = 0;

            private boolean computeNext() {
                computed = false;

                int start = i;
                for (; i < length; i++) {
                    boolean leftBit = leftSplit.get(i);
                    boolean rightBit = rightSplit.get(i);

                    if (leftBit)
                        start = i;

                    if (rightBit) {
                        computed = true;
                        next[0] = start;
                        next[1] = i - start + 1;

                        i++;
                        break;
                    }
                }

                return computed;
            }

            @Override
            public boolean hasNext() {
                return computed || computeNext();
            }

            @Override
            public int[] next() {
                if (computed) {
                    computed = false;
                    return next;
                } else {
                    return null;
                }
            }
        };
    }

    public int length() {
        return length;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < length; i++)
            string.append(leftSplit.get(i) ? '1' : '0');
        string.append('\n');
        for (int i = 0; i < length; i++)
            string.append(rightSplit.get(i) ? '1' : '0');
        return string.toString();
    }

    public static void main(String[] args) {
        String test = "This is {0}\t";
        TokenMask mask = new TokenMask(test.length());

        mask.setToken(0, 4);
        mask.setToken(5, 2);
        mask.setToken(8, 1);
        mask.setToken(9, 1);
        mask.setToken(10, 1);

        System.out.println(test);
        System.out.println(mask);

        for (int[] token : mask)
            System.out.println("'" + test.substring(token[0], token[0] + token[1]) + "'");
        System.out.println();

        mask.setToken(8, 3);

        System.out.println(test);
        System.out.println(mask);

        for (int[] token : mask)
            System.out.println("'" + test.substring(token[0], token[0] + token[1]) + "'");
        System.out.println();

        mask.setToken(8, 1);

        System.out.println(test);
        System.out.println(mask);

        for (int[] token : mask)
            System.out.println("'" + test.substring(token[0], token[0] + token[1]) + "'");
        System.out.println();
    }

}
