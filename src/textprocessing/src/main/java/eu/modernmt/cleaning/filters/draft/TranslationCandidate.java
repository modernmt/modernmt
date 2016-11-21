package eu.modernmt.cleaning.filters.draft;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * Created by davide on 18/11/16.
 */
class TranslationCandidate implements Comparable<TranslationCandidate> {

    static long hash(String string) {
        int length = string.length();

        String sx, dx;

        if (length > 1) {
            int hlen = length / 2;

            sx = string.substring(0, hlen);
            dx = string.substring(hlen, length);
        } else {
            sx = string;
            dx = "";
        }

        return (long) (sx.hashCode()) << 32 | (dx.hashCode()) & 0xFFFFFFFFL;
    }

    private final long hash;
    private final Date timestamp;

    TranslationCandidate(String target, Date timestamp) {
        this.hash = hash(target);
        this.timestamp = timestamp;
    }

    long timeDiff(TranslationCandidate other) {
        return timestamp.getTime() - other.timestamp.getTime();
    }

    long getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TranslationCandidate that = (TranslationCandidate) o;

        return hash == that.hash;

    }

    @Override
    public int hashCode() {
        return (int) (hash ^ (hash >>> 32));
    }

    @Override
    public int compareTo(@NotNull TranslationCandidate o) {
        return timestamp.compareTo(o.timestamp);
    }

}
