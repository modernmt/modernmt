package eu.modernmt.processing.tags.projection;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Coverage implements Iterable<Integer> {

    private ArrayList<Integer> positions;

    Coverage() {
        this.positions = new ArrayList<>();
    }

    Coverage(int size) {
        this.positions = new ArrayList<>(size);
    }

    Coverage(Coverage c) {
        this.positions = new ArrayList<>(c.size());
        this.positions.addAll(c.getPositions());
    }

    protected boolean isEmpty() {
        return this.positions.isEmpty();
    }

    protected int get(int i) {
        return this.positions.get(i);
    }

    protected boolean add(int pos) {
        if (!this.positions.contains(pos)) {
            return this.positions.add(pos);
        } else {
            return false;
        }
    }

    boolean remove(Integer chosenP) {
        return this.positions.remove(chosenP);
    }

    void clear() {
        this.positions.clear();
    }

    void addAll(Coverage c) {
        this.positions.addAll(c.getPositions());
    }

    static Coverage intersection(Coverage c1, Coverage c2) {
        // create the intersection between c1 and c2
        Coverage intersection = new Coverage();
        for (int pos : c1) {
            if (c2.contains(pos)) {
                intersection.add(pos);
            }
        }
        return intersection;
    }


    static Coverage difference(Coverage c1, Coverage c2) {
        // create the difference c1 / c2
        Coverage difference = new Coverage();
        for (int pos : c1) {
            if (!c2.contains(pos)) {
                difference.add(pos);
            }
        }

        return difference;
    }

    boolean contains(Integer pos) {
        return this.positions.contains(pos);
    }

    static Coverage contiguous(Coverage c) {
        Coverage contiguous = new Coverage(c.size());
        if (c.size() > 0) {
            for (int i = c.getMin(); i <= c.getMax(); i++) {
                contiguous.add(i);
            }
        }
        return contiguous;
    }

    void sort() {
        Collections.sort(this.positions);
    }

    ArrayList<Integer> getPositions() {
        return this.positions;
    }

    int size() {
        return positions.size();
    }

    int getMin() {
        return Collections.min(this.positions);
    }

    int getMax() {
        return Collections.max(this.positions);
    }

    public static int choosePosition(Coverage c1, Coverage c2) {
        //there is at least one point in the overlap
        int min1 = c1.getMin();
        int min2 = c2.getMin();
        return Math.max(min1, min2);
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < positions.size();
            }

            @Override
            public Integer next() {
                int pos = positions.get(index);
                index++;
                return pos;
            }
        };
    }

    public String toString() {
        return this.positions.toString();
    }

    public int first() {
        return this.positions.get(0);
    }

    public int last() {
        return this.positions.get(size() - 1);
    }

    void uniq() {
        Set<Integer> set = new LinkedHashSet<>(this.positions);

        // Clear the list
        this.positions.clear();

        // add the elements of set
        // with no duplicates to the list
        this.positions.addAll(set);
    }

    void retainAll(Coverage targetRightToken) {
        this.positions.retainAll(targetRightToken.getPositions());
    }
}
