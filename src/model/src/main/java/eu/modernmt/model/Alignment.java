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

    public static Alignment fromAlignmentPairs(int[][] pairs) {
        int[] sourceIndexes = new int[pairs.length];
        int[] targetIndexes = new int[pairs.length];

        for (int i = 0; i < pairs.length; i++) {
            sourceIndexes[i] = pairs[i][0];
            targetIndexes[i] = pairs[i][1];
        }

        return new Alignment(sourceIndexes, targetIndexes);
    }

    public Alignment(int[] sourceIndexes, int[] targetIndexes) {
        this.sourceIndexes = sourceIndexes;
        this.targetIndexes = targetIndexes;
    }

    public int size() {
        return sourceIndexes.length;
    }

    public int[] getWordsAlignedWithSource(int index) {
        int[] buffer = new int[sourceIndexes.length];
        int size = 0;

        for (int i = 0; i < sourceIndexes.length; i++) {
            if (sourceIndexes[i] == index)
                buffer[size++] = targetIndexes[i];
        }

        int[] result = new int[size];
        if (size > 0)
            System.arraycopy(buffer, 0, result, 0, size);

        return result;
    }

    public int[] getWordsAlignedWithTarget(int index) {
        int[] buffer = new int[targetIndexes.length];
        int size = 0;

        for (int i = 0; i < targetIndexes.length; i++) {
            if (targetIndexes[i] == index)
                buffer[size++] = sourceIndexes[i];
        }

        int[] result = new int[size];
        if (size > 0)
            System.arraycopy(buffer, 0, result, 0, size);

        return result;
    }

    public Alignment getInverse() {
        return new Alignment(targetIndexes, sourceIndexes);
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

        Alignment ints = (Alignment) o;

        if (!Arrays.equals(sourceIndexes, ints.sourceIndexes)) return false;
        return Arrays.equals(targetIndexes, ints.targetIndexes);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(sourceIndexes);
        result = 31 * result + Arrays.hashCode(targetIndexes);
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
