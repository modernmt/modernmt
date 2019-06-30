package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.CorpusFilter;
import eu.modernmt.lang.Language2;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by davide on 17/11/16.
 */
public class RareNgramFilter implements CorpusFilter {

    private static final int MIN_CORPUS_LINES = 500;
    private static final int MIN_SENTENCE_LENGTH = 30;

    private HashSet<String> words = null;

    @Override
    public Initializer getInitializer(Language2 language) {
        return new Initializer() {

            private final HashMap<String, Counter> map = new HashMap<>();
            private int lines = 0;

            @Override
            public void onBegin() {
                words = null;
            }

            @Override
            public void onLine(String line, int index) {
                lines++;

                line = normalize(line);

                if (line.length() < MIN_SENTENCE_LENGTH)
                    return;

                for (String token : tokenize(line))
                    map.computeIfAbsent(token, key -> new Counter()).count++;
            }

            @Override
            public void onEnd() {
                words = lines < MIN_CORPUS_LINES ? null : filterCounts(map, .9);
            }

            private HashSet<String> filterCounts(HashMap<String, Counter> vocabulary, double threshold) {
                ArrayList<Entry> entries = new ArrayList<>(vocabulary.size());
                for (Map.Entry<String, Counter> e : vocabulary.entrySet())
                    entries.add(new Entry(e.getKey(), e.getValue().count));

                Collections.sort(entries);
                Collections.reverse(entries);

                double size = 0;
                for (Entry e : entries)
                    size += e.count;

                HashSet<String> result = new HashSet<>();

                double accumulator = 0;
                for (Entry e : entries) {
                    accumulator += e.count;
                    result.add(e.term);

                    if ((accumulator / size) >= threshold)
                        break;
                }

                return result;
            }

        };
    }

    private static final Pattern SKIP_CHARS_REGEX = Pattern.compile("[\\p{Punct}\\s]+");
    private static final Pattern DIGITS_REGEX = Pattern.compile("[0-9]");

    private static String normalize(String line) {
        line = line.toLowerCase();
        line = SKIP_CHARS_REGEX.matcher(line).replaceAll("");
        line = DIGITS_REGEX.matcher(line).replaceAll("0");
        return line;
    }

    private static String[] tokenize(String string) {
        int length = string.length();
        if (length < 3)
            return new String[0];

        int size = length - length % 3 - 2;

        String[] result = new String[size];

        if (size > 0) {
            for (int i = 0; i < size; i++)
                result[i] = string.substring(i, i + 3);
        }

        return result;
    }

    @Override
    public boolean accept(String line, int index) {
        if (words == null)
            return true;

        line = normalize(line);

        if (line.length() < MIN_SENTENCE_LENGTH)
            return true;

        return match(line) >= .3;
    }

    private double match(String line) {
        int matches = 0;
        int length = 0;

        for (String token : tokenize(line)) {
            length++;
            if (words.contains(token))
                matches++;
        }

        return matches / ((double) length);
    }

    @Override
    public void clear() {
        words = null;
    }

    private static final class Counter {

        public long count = 0;

    }

    private static final class Entry implements Comparable<Entry> {

        public final String term;
        public final long count;

        public Entry(String term, long count) {
            this.term = term;
            this.count = count;
        }

        @Override
        public int compareTo(@NotNull Entry o) {
            return Long.compare(count, o.count);
        }

        @Override
        public String toString() {
            return term + '(' + count + ')';
        }
    }

}
