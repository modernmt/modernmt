package eu.modernmt.cleaning.filters.ngrams;

import eu.modernmt.model.corpus.MultilingualCorpus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by davide on 28/08/17.
 */
class Vocabulary {

    public static final int MIN_CORPUS_LINES = 500;
    public static final int MIN_SENTENCE_LENGTH = 20;

    static class Builder {

        private final HashMap<String, Counter> source = new HashMap<>();
        private final HashMap<String, Counter> target = new HashMap<>();
        private int lines = 0;

        public void add(MultilingualCorpus.StringPair pair) {
            lines++;
            add(pair.source, source);
            add(pair.target, target);
        }

        private static void add(String line, HashMap<String, Counter> vocabulary) {
            line = normalize(line);

            for (String token : tokenize(line))
                vocabulary.computeIfAbsent(token, key -> new Counter()).count++;
        }

        public Vocabulary build(double threshold) {
            if (lines < MIN_CORPUS_LINES)
                return new Vocabulary();

            HashSet<String> vocabularySource = filterCounts(source, threshold);
            HashSet<String> vocabularyTarget = filterCounts(target, threshold);
            return new Vocabulary(vocabularySource, vocabularyTarget);
        }

        private static HashSet<String> filterCounts(HashMap<String, Counter> vocabulary, double threshold) {
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

    private static final Pattern SKIP_CHARS_REGEX = Pattern.compile("[\\p{Punct}\\s]+");
    private static final Pattern DIGITS_REGEX = Pattern.compile("[0-9]");

    private static String normalize(String line) {
        line = line.toLowerCase();
        line = SKIP_CHARS_REGEX.matcher(line).replaceAll(" ");
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

    private final HashSet<String> source;
    private final HashSet<String> target;

    private Vocabulary() {
        this(null, null);
    }

    private Vocabulary(HashSet<String> source, HashSet<String> target) {
        this.source = source;
        this.target = target;
    }

    public boolean accept(MultilingualCorpus.StringPair pair, double threshold) {
        if (source == null || target == null)
            return true;

        String sourceLine = normalize(pair.source);
        String targetLine = normalize(pair.target);

        if (sourceLine.length() < MIN_SENTENCE_LENGTH || targetLine.length() < MIN_SENTENCE_LENGTH)
            return true;

        return match(sourceLine, source) >= threshold && match(sourceLine, target) >= threshold;
    }

    private double match(String line, HashSet<String> terms) {
        int matches = 0;
        int length = 0;

        for (String token : tokenize(line)) {
            length++;
            if (terms.contains(token))
                matches++;
        }

        return matches / ((double) length);
    }
}
