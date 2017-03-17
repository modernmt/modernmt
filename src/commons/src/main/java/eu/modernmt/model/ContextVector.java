package eu.modernmt.model;

import java.io.Serializable;
import java.util.*;

/**
 * Created by davide on 18/01/17.
 */
public class ContextVector implements Iterable<ContextVector.Entry>, Serializable {

    public static class Builder {

        private final HashMap<Domain, Float> entries;
        private int limit = 0;

        public Builder(int initialCapacity) {
            entries = new HashMap<>(initialCapacity);
        }

        public Builder() {
            entries = new HashMap<>();
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public void add(int domain, float score) {
            this.add(new Domain(domain), score);
        }

        public void add(Domain domain, float score) {
            entries.put(domain, score);
        }

        public ContextVector build() {
            List<Entry> list = new ArrayList<>(this.entries.size());
            for (Map.Entry<Domain, Float> e : this.entries.entrySet())
                list.add(new Entry(e.getKey(), e.getValue()));

            Collections.sort(list);
            Collections.reverse(list);

            if (limit > 0 && list.size() > limit)
                list = list.subList(0, limit);

            return new ContextVector(list.toArray(new Entry[list.size()]));
        }
    }

    public static class Entry implements Comparable<Entry>, Serializable {

        public final Domain domain;
        public final float score;

        private Entry(Domain domain, float score) {
            this.domain = domain;
            this.score = score;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (Float.compare(entry.score, score) != 0) return false;
            return domain.equals(entry.domain);
        }

        @Override
        public int hashCode() {
            int result = domain.hashCode();
            result = 31 * result + (score != +0.0f ? Float.floatToIntBits(score) : 0);
            return result;
        }


        @Override
        public int compareTo(Entry o) {
            return Float.compare(score, o.score);
        }
    }

    private final Entry[] entries;

    private ContextVector(Entry[] entries) {
        this.entries = entries;
    }

    public int size() {
        return entries.length;
    }

    @Override
    public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {

            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < entries.length;
            }

            @Override
            public Entry next() {
                return entries[i++];
            }
        };
    }
}
