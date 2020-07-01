package eu.modernmt.model;

import java.util.HashSet;

public class AlignmentPoint {

    public int source;
    public int target;

    public static HashSet<AlignmentPoint> parse(Alignment alignment) {
        HashSet<AlignmentPoint> result = new HashSet<>(alignment.size());
        for (int[] point : alignment)
            result.add(new AlignmentPoint(point[0], point[1]));
        return result;
    }

    public AlignmentPoint() {
        this(0, 0);
    }

    public AlignmentPoint(int source, int target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlignmentPoint that = (AlignmentPoint) o;

        if (source != that.source) return false;
        return target == that.target;
    }

    @Override
    public int hashCode() {
        int result = source;
        result = 31 * result + target;
        return result;
    }
}
