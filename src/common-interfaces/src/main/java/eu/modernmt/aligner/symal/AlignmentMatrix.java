package eu.modernmt.aligner.symal;

import eu.modernmt.model.Alignment;

/**
 * Created by davide on 20/05/16.
 */
class AlignmentMatrix {

    private byte[] matrix;
    private int forwardSize;
    private int backwardSize;

    public static AlignmentMatrix build(Alignment forward, Alignment backward) {
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
        this.matrix = new byte[forwardSize * backwardSize];
        this.forwardSize = forwardSize;
        this.backwardSize = backwardSize;
    }

    public AlignmentMatrix and(Alignment alignment) {
        for (int[] el : alignment) {
            int i = el[0] * backwardSize + el[1];
            matrix[i]++;
        }

        for (int i = 0; i < matrix.length; i++)
            matrix[i] = (byte) (matrix[i] == 2 ? 1 : 0);

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

    public AlignmentMatrix or(Alignment alignments) {
        for (int[] el : alignments) {
            int i = el[0] * backwardSize + el[1];
            matrix[i] = 1;
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
        return matrix[f * backwardSize + b] > 0;
    }

    public void set(int f, int b) {
        matrix[f * backwardSize + b] = 1;
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
            if (matrix[f + b] > 0)
                return true;
        }

        return false;
    }

    public boolean isTargetWordAligned(int b) {
        for (int f = 0; f < forwardSize; f++) {
            if (matrix[f * backwardSize + b] > 0)
                return true;
        }

        return false;
    }

    public Alignment toAlignment() {
        int size = 0;

        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] > 0)
                size++;
        }

        int j = 0;
        int[] source = new int[size];
        int[] target = new int[size];

        for (int f = 0; f < forwardSize; f++) {
            for (int b = 0; b < backwardSize; b++) {
                int i = f * backwardSize + b;

                if (matrix[i] > 0) {
                    source[j] = f;
                    target[j] = b;
                    j++;
                }
            }
        }

        return new Alignment(source, target);
    }

}
