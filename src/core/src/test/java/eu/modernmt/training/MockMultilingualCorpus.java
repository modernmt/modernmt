package eu.modernmt.training;

import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by davide on 14/03/16.
 */
public class MockMultilingualCorpus extends BaseMultilingualCorpus {

    private static final long MOCK_EPOCH = new Date().getTime();
    private static final LanguageDirection MOCK_LANGUAGE = new LanguageDirection(Language2.ENGLISH, Language2.ITALIAN);

    private final String name;
    private final StringPair[] lines;

    public static StringPair pair(String source, String target, long date) {
        return new StringPair(MOCK_LANGUAGE, source, target, new Date(MOCK_EPOCH + (date * 60L * 1000L)));
    }

    private static StringPair[] parse(String[][] lines) {
        StringPair[] result = new StringPair[lines.length];
        for (int i = 0; i < lines.length; i++)
            result[i] = new StringPair(MOCK_LANGUAGE, lines[i][0], lines[i][1]);

        return result;
    }

    public MockMultilingualCorpus(String[][] lines) {
        this(parse(lines));
    }

    public MockMultilingualCorpus(StringPair[] lines) {
        this("None", lines);
    }

    public MockMultilingualCorpus(String name, String[][] lines) {
        this(name, parse(lines));
    }

    public MockMultilingualCorpus(String name, StringPair[] lines) {
        this.name = name;
        this.lines = lines;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MultilingualLineReader getContentReader() throws IOException {
        return new MultilingualLineReader() {

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
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    public static MockMultilingualCorpus drain(MultilingualLineReader reader) throws IOException {
        ArrayList<StringPair> pairs = new ArrayList<>();

        StringPair pair;
        while ((pair = reader.read()) != null) {
            pairs.add(pair);
        }

        return new MockMultilingualCorpus(pairs.toArray(new StringPair[pairs.size()]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MockMultilingualCorpus corpus = (MockMultilingualCorpus) o;

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
