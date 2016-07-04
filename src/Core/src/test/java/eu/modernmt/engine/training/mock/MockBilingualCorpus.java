package eu.modernmt.engine.training.mock;

import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
public class MockBilingualCorpus implements BilingualCorpus {

    private static final long MOCK_EPOCH = new Date().getTime();

    private final String name;
    private final Locale source;
    private final Locale target;
    private final StringPair[] lines;

    public static StringPair pair(String source, String target, long date) {
        return new StringPair(source, target, new Date(MOCK_EPOCH + (date * 60L * 1000L)));
    }

    private static StringPair[] parse(String[][] lines) {
        StringPair[] result = new StringPair[lines.length];
        for (int i = 0; i < lines.length; i++)
            result[i] = new StringPair(lines[i][0], lines[i][1]);

        return result;
    }

    public MockBilingualCorpus(String[][] lines) {
        this(parse(lines));
    }

    public MockBilingualCorpus(StringPair[] lines) {
        this("None", Locale.ENGLISH, Locale.ITALIAN, lines);
    }

    public MockBilingualCorpus(String name, Locale source, Locale target, String[][] lines) {
        this(name, source, target, parse(lines));
    }

    public MockBilingualCorpus(String name, Locale source, Locale target, StringPair[] lines) {
        this.name = name;
        this.source = source;
        this.target = target;
        this.lines = lines;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getSourceLanguage() {
        return source;
    }

    @Override
    public Locale getTargetLanguage() {
        return target;
    }

    @Override
    public int getLineCount() throws IOException {
        return lines.length;
    }

    @Override
    public BilingualLineReader getContentReader() throws IOException {
        return new BilingualLineReader() {

            private int i = 0;

            @Override
            public StringPair read() throws IOException {
                return i < lines.length ? lines[i++] : null;
            }

            @Override
            public void close() throws IOException {
                // Nothing to do;
            }
        };
    }

    @Override
    public BilingualLineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Corpus getSourceCorpus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Corpus getTargetCorpus() {
        throw new UnsupportedOperationException();
    }

    public static MockBilingualCorpus drain(BilingualLineReader reader) throws IOException {
        ArrayList<StringPair> pairs = new ArrayList<>();

        StringPair pair;
        while ((pair = reader.read()) != null) {
            pairs.add(pair);
        }

        return new MockBilingualCorpus(pairs.toArray(new StringPair[pairs.size()]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MockBilingualCorpus corpus = (MockBilingualCorpus) o;

        return Arrays.equals(lines, corpus.lines);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(lines);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (StringPair pair : lines) {
            builder.append(pair.source);
            builder.append(',');
            builder.append(pair.target);
            builder.append('(');
            if (pair.timestamp != null)
                builder.append(pair.timestamp.toString());
            builder.append(')');
            builder.append(' ');
        }

        return builder.toString();
    }
}
