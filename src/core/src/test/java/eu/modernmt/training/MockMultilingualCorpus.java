package eu.modernmt.training;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.TUReader;
import eu.modernmt.model.corpus.TUWriter;
import eu.modernmt.model.corpus.TranslationUnit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by davide on 14/03/16.
 */
public class MockMultilingualCorpus extends BaseMultilingualCorpus {

    private static final long MOCK_EPOCH = new Date().getTime();
    private static final LanguageDirection MOCK_LANGUAGE = new LanguageDirection(Language.ENGLISH, Language.ITALIAN);

    private final String name;
    private final TranslationUnit[] tus;

    public static TranslationUnit tu(String source, String target, long date) {
        return new TranslationUnit(MOCK_LANGUAGE, source, target, new Date(MOCK_EPOCH + (date * 60L * 1000L)));
    }

    private static TranslationUnit[] parse(String[][] lines) {
        TranslationUnit[] result = new TranslationUnit[lines.length];
        for (int i = 0; i < lines.length; i++)
            result[i] = new TranslationUnit(MOCK_LANGUAGE, lines[i][0], lines[i][1]);

        return result;
    }

    public MockMultilingualCorpus(String[][] tus) {
        this(parse(tus));
    }

    public MockMultilingualCorpus(TranslationUnit[] tus) {
        this("None", tus);
    }

    public MockMultilingualCorpus(String name, String[][] tus) {
        this(name, parse(tus));
    }

    public MockMultilingualCorpus(String name, TranslationUnit[] tus) {
        this.name = name;
        this.tus = tus;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TUReader getContentReader() {
        return new TUReader() {

            private int i = 0;

            @Override
            public TranslationUnit read() {
                return i < tus.length ? tus[i++] : null;
            }

            @Override
            public void close() {
                // Nothing to do;
            }
        };
    }

    @Override
    public TUWriter getContentWriter(boolean append) {
        throw new UnsupportedOperationException();
    }

    public static MockMultilingualCorpus drain(TUReader reader) throws IOException {
        ArrayList<TranslationUnit> tus = new ArrayList<>();

        TranslationUnit tu;
        while ((tu = reader.read()) != null) {
            tus.add(tu);
        }

        return new MockMultilingualCorpus(tus.toArray(new TranslationUnit[0]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MockMultilingualCorpus corpus = (MockMultilingualCorpus) o;

        return Arrays.equals(tus, corpus.tus);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(tus);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (TranslationUnit tu : tus) {
            builder.append(tu.source);
            builder.append(',');
            builder.append(tu.target);
            builder.append('(');
            if (tu.timestamp != null)
                builder.append(tu.timestamp.toString());
            builder.append(')');
            builder.append(' ');
        }

        return builder.toString();
    }
}
