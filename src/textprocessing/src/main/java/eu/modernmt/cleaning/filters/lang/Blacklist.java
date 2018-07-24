package eu.modernmt.cleaning.filters.lang;

import java.util.ArrayList;

/**
 * Created by davide on 27/12/17.
 */
class Blacklist {

    private final ArrayList<Range> data = new ArrayList<>();
    private int lastCheckIndex = 0;

    public void add(int begin, int end) {
        data.add(new Range(begin, end));
    }

    public int size() {
        int size = 0;
        for (Range range : data)
            size += range.size();
        return size;
    }

    public boolean contains(int index) {
        if (data.isEmpty())
            return false;

        if (lastCheckIndex >= data.size())
            lastCheckIndex = 0;

        while (index > data.get(lastCheckIndex).end) {
            lastCheckIndex++;
            if (lastCheckIndex >= data.size()) {
                lastCheckIndex--;
                break;
            }
        }

        while (index < data.get(lastCheckIndex).begin) {
            lastCheckIndex--;
            if (lastCheckIndex < 0) {
                lastCheckIndex++;
                break;
            }
        }

        return data.get(lastCheckIndex).contains(index);
    }

    static class Range {

        final int begin;
        final int end;

        Range(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        public boolean contains(int index) {
            return begin <= index && index <= end;
        }

        public int size() {
            return this.end - this.begin;
        }
    }
}
