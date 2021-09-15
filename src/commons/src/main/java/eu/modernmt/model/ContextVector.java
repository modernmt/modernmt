package eu.modernmt.model;

import java.io.Serializable;
import java.util.*;

/**
 * Created by davide on 18/01/17.
 */
public class ContextVector implements Iterable<ContextVector.Entry>, Serializable {

    public static class Builder {

        private final HashMap<Memory, Float> entries;
        private int limit = 0;

        public Builder(int initialCapacity) {
            entries = new HashMap<>(initialCapacity);
        }

        public Builder() {
            entries = new HashMap<>();
        }

        public Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder add(long memory, float score) {
            this.add(new Memory(memory), score);
            return this;
        }

        public Builder add(Memory memory, float score) {
            if (score > 0.f)
                entries.put(memory, score);
            return this;
        }

        public ContextVector build() {
            List<Entry> list = new ArrayList<>(this.entries.size());
            for (Map.Entry<Memory, Float> e : this.entries.entrySet())
                list.add(new Entry(e.getKey(), e.getValue()));

            Collections.sort(list);
            Collections.reverse(list);

            if (limit > 0 && list.size() > limit)
                list = list.subList(0, limit);

            return new ContextVector(list.toArray(new Entry[0]));
        }
    }

    public static ContextVector fromString(String string) throws IllegalArgumentException {
        String[] elements = string.split(",");

        ContextVector.Builder builder = new ContextVector.Builder(elements.length);

        for (String element : elements) {
            String[] kv = element.split(":");

            if (kv.length != 2)
                throw new IllegalArgumentException(string);

            long id;
            UUID owner;
            float score;

            try {
                int idx = kv[0].indexOf('@');
                if (idx < 0) {
                    id = Long.parseLong(kv[0]);
                    owner = null;
                } else {
                    String ownerString = kv[0].substring(0, idx);
                    String idString = kv[0].substring(idx + 1);

                    id = Long.parseLong(idString);

                    String[] uc = ownerString.split("/");
                    if (uc.length != 2)
                        throw new IllegalArgumentException(string);

                    long msb = Long.parseLong(uc[0]);
                    long lsb = Long.parseLong(uc[1]);
                    owner = new UUID(msb, lsb);
                }

                score = Float.parseFloat(kv[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(string);
            }

            if (id < 1)
                throw new IllegalArgumentException(string);

            if (score < 0.f || score > 1.f)
                throw new IllegalArgumentException(string);

            builder.add(new Memory(id, owner, null), score);
        }

        return builder.build();
    }

    public static class Entry implements Comparable<Entry>, Serializable {

        public final Memory memory;
        public final float score;

        private Entry(Memory memory, float score) {
            this.memory = memory;
            this.score = score;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (Float.compare(entry.score, score) != 0) return false;
            return memory.equals(entry.memory);
        }

        @Override
        public int hashCode() {
            int result = memory.hashCode();
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

    public boolean isEmpty() {
        return entries.length == 0;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < entries.length; i++) {
            if (i > 0)
                builder.append(',');
            builder.append(entries[i].memory.getId());
            builder.append(':');
            builder.append(entries[i].score);
        }

        return builder.toString();
    }
}
