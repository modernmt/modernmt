package eu.modernmt.cleaning.filters.draft;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * Created by davide on 18/11/16.
 */
class TranslationCandidate implements Comparable<TranslationCandidate> {

    private final int index;
    private final Date timestamp;

    TranslationCandidate(int index, Date timestamp) {
        this.index = index;
        this.timestamp = timestamp;
    }

    long timeDiff(TranslationCandidate other) {
        return timestamp.getTime() - other.timestamp.getTime();
    }

    int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TranslationCandidate that = (TranslationCandidate) o;

        return index == that.index;

    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public int compareTo(@NotNull TranslationCandidate o) {
        return timestamp.compareTo(o.timestamp);
    }

    @Override
    public String toString() {
        return "{POS:" + index + ", T=" + timestamp + "}";
    }
}
