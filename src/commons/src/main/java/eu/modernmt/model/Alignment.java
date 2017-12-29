package eu.modernmt.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by davide on 31/08/16.
 */
public class Alignment implements Iterable<int[]>, Serializable {

    private final int[] sourceIndexes;
    private final int[] targetIndexes;
    private final float score;

    public static Alignment fromAlignmentPairs(int[][] pairs) {
        return fromAlignmentPairs(pairs, 0);
    }

    public static Alignment fromAlignmentPairs(int[][] pairs, float score) {
        int[] sourceIndexes = new int[pairs.length];
        int[] targetIndexes = new int[pairs.length];

        for (int i = 0; i < pairs.length; i++) {
            sourceIndexes[i] = pairs[i][0];
            targetIndexes[i] = pairs[i][1];
        }

        return new Alignment(sourceIndexes, targetIndexes, score);
    }

    public Alignment(int[] sourceIndexes, int[] targetIndexes) {
        this(sourceIndexes, targetIndexes, 0);
    }

    public Alignment(int[] sourceIndexes, int[] targetIndexes, float score) {
        this.sourceIndexes = sourceIndexes;
        this.targetIndexes = targetIndexes;
        this.score = score;
    }

    public int[] getSourceIndexes() {
        return sourceIndexes;
    }

    public int[] getTargetIndexes() {
        return targetIndexes;
    }

    public int size() {
        return sourceIndexes.length;
    }

    public float getScore() {
        return score;
    }

    public Alignment getInverse() {
        return new Alignment(targetIndexes, sourceIndexes, score);
    }

    @Override
    public Iterator<int[]> iterator() {
        return new Iterator<int[]>() {
            private int index = 0;
            private final int[] container = new int[2];

            @Override
            public boolean hasNext() {
                return index < sourceIndexes.length;
            }

            @Override
            public int[] next() {
                container[0] = sourceIndexes[index];
                container[1] = targetIndexes[index];

                index++;

                return container;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alignment alignment = (Alignment) o;

        if (Float.compare(alignment.score, score) != 0) return false;
        if (!Arrays.equals(sourceIndexes, alignment.sourceIndexes)) return false;
        return Arrays.equals(targetIndexes, alignment.targetIndexes);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(sourceIndexes);
        result = 31 * result + Arrays.hashCode(targetIndexes);
        result = 31 * result + (score != +0.0f ? Float.floatToIntBits(score) : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < sourceIndexes.length; i++) {
            if (i > 0)
                builder.append(' ');
            builder.append(sourceIndexes[i]);
            builder.append('-');
            builder.append(targetIndexes[i]);
        }

        return builder.toString();
    }
}
