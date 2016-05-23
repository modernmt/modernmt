package eu.modernmt.aligner.symal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created by davide on 20/05/16.
 */
class AlignmentMatrix {

    private boolean[] matrix;
    private int forwardSize;
    private int backwardSize;

    public static AlignmentMatrix build(int[][] forward, int[][] backward) {
        int maxForward = -1;
        int maxBackward = -1;

        for (int[] el : forward) {
            if (el[0] > maxForward) maxForward = el[0];
            if (el[1] > maxBackward) maxBackward = el[1];
        }
        for (int[] el : backward) {
            if (el[0] > maxForward) maxForward = el[0];
            if (el[1] > maxBackward) maxBackward = el[1];
        }

        return new AlignmentMatrix(maxForward + 1, maxBackward + 1);
    }

    private AlignmentMatrix(int forwardSize, int backwardSize) {
        this.matrix = new boolean[forwardSize * backwardSize];
        this.forwardSize = forwardSize;
        this.backwardSize = backwardSize;
    }

    public AlignmentMatrix and(int[][] alignments) {
        Arrays.sort(alignments, Sorter.instance);

        int lastIndex = -1;

        for (int[] el : alignments) {
            int i = el[0] * backwardSize + el[1];

            for (int j = lastIndex + 1; j < i; j++)
                matrix[j] = false;

            matrix[i] &= true;
            lastIndex = i;
        }

        for (int j = lastIndex + 1; j < matrix.length; j++)
            matrix[j] = false;

        return this;
    }

    public AlignmentMatrix and(AlignmentMatrix matrix) {
        if (matrix.backwardSize != backwardSize || matrix.forwardSize != forwardSize)
            throw new IllegalArgumentException("Mismatching matrix sizes");

        for (int i = 0; i < matrix.matrix.length; i++) {
            this.matrix[i] &= matrix.matrix[i];
        }

        return this;
    }

    public AlignmentMatrix or(int[][] alignments) {
        for (int[] el : alignments) {
            int i = el[0] * backwardSize + el[1];
            matrix[i] = true;
        }

        return this;
    }

    public AlignmentMatrix or(AlignmentMatrix matrix) {
        if (matrix.backwardSize != backwardSize || matrix.forwardSize != forwardSize)
            throw new IllegalArgumentException("Mismatching matrix sizes");

        for (int i = 0; i < matrix.matrix.length; i++) {
            this.matrix[i] |= matrix.matrix[i];
        }

        return this;
    }

    public boolean get(int f, int b) {
        return matrix[f * backwardSize + b];
    }

    public void set(int f, int b) {
        matrix[f * backwardSize + b] = true;
    }

    public int getForwardSize() {
        return forwardSize;
    }

    public int getBackwardSize() {
        return backwardSize;
    }

    public boolean isSourceWordAligned(int f) {
        f = f * backwardSize;

        for (int b = 0; b < backwardSize; b++) {
            if (matrix[f + b])
                return true;
        }

        return false;
    }

    public boolean isTargetWordAligned(int b) {
        for (int f = 0; f < forwardSize; f++) {
            if (matrix[f * backwardSize + b])
                return true;
        }

        return false;
    }

    public int[][] toArray() {
        ArrayList<int[]> list = new ArrayList<>();
        for (int f = 0; f < forwardSize; f++) {
            for (int b = 0; b < backwardSize; b++) {
                int i = f * backwardSize + b;

                if (matrix[i])
                    list.add(new int[]{f, b});
            }
        }

        return list.toArray(new int[list.size()][]);
    }

    static class Sorter implements Comparator<int[]> {

        static Sorter instance = new Sorter();

        @Override
        public int compare(int[] o1, int[] o2) {
            int c = Integer.compare(o1[0], o2[0]);
            return c == 0 ? Integer.compare(o1[1], o2[1]) : c;
        }

    }

}
