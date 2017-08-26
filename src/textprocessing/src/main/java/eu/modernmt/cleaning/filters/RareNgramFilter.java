package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.MultilingualCorpusFilter;
import eu.modernmt.cleaning.filters.util.Sequence;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by davide on 17/11/16.
 */
public class RareNgramFilter implements MultilingualCorpusFilter {

    public static final int MIN_CORPUS_LINES = 1000;

    private final HashMap<LanguagePair, Vocabulary> ngrams = new HashMap<>();

    private static String normalize(String line) {
        return line.toLowerCase().replaceAll("[0-9\\p{Punct}\\s]+", "");
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

    private static void add(String line, HashMap<String, Counter> vocabulary) {
        line = normalize(line);

        for (String token : tokenize(line))
            vocabulary.computeIfAbsent(token, key -> new Counter()).count++;
    }

    private static void filterVocabulary(HashMap<String, Counter> vocabulary, HashSet<String> output) {
        Sequence sequence = new Sequence();
        for (Counter value : vocabulary.values())
            sequence.add(value.count);

        long threshold = Math.round(sequence.getAverage());

        for (Map.Entry<String, Counter> entry : vocabulary.entrySet()) {
            if (entry.getValue().count > threshold)
                output.add(entry.getKey());
        }
    }

    @Override
    public FilterInitializer getInitializer() {
        return new FilterInitializer() {

            private final HashMap<LanguagePair, VocabularyBuilder> vocabs = new HashMap<>();

            @Override
            public void onBegin() {
                clear();
            }

            @Override
            public void onPair(MultilingualCorpus corpus, MultilingualCorpus.StringPair pair, int index) throws IOException {
                VocabularyBuilder builder = vocabs.computeIfAbsent(pair.language, key -> new VocabularyBuilder());
                builder.lines++;
                add(pair.source, builder.source);
                add(pair.target, builder.target);
            }

            @Override
            public void onEnd() {
                for (Map.Entry<LanguagePair, VocabularyBuilder> entry : vocabs.entrySet()) {
                    Vocabulary vocab = new Vocabulary();
                    vocab.lines = entry.getValue().lines;
                    filterVocabulary(entry.getValue().source, vocab.source);
                    filterVocabulary(entry.getValue().target, vocab.target);

                    ngrams.put(entry.getKey(), vocab);
                }
            }

        };
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException {
        Vocabulary vocabulary = ngrams.get(pair.language);
        return vocabulary.lines < MIN_CORPUS_LINES ||
                (accept(pair.source, vocabulary.source) && accept(pair.target, vocabulary.target));
    }

    private static boolean accept(String line, HashSet<String> ngrams) {
        int rare = 0;
        int common = 0;

        line = normalize(line);

        if (line.length() < 10)
            return true;

        for (String token : tokenize(line)) {
            if (ngrams.contains(token))
                common++;
            else
                rare++;
        }

        return (common / (double)(common + rare)) > .25;
    }

    @Override
    public void clear() {
        ngrams.clear();
    }

    private static final class Counter {

        public long count = 0;

    }

    private static final class VocabularyBuilder {

        public HashMap<String, Counter> source = new HashMap<>();
        public HashMap<String, Counter> target = new HashMap<>();
        public int lines = 0;

    }

    private static final class Vocabulary {

        public HashSet<String> source = new HashSet<>();
        public HashSet<String> target = new HashSet<>();
        public int lines = 0;

    }

}
