package eu.modernmt.cleaning;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.BaseCorpus;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.CorpusWrapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;

public class FilteredCorpus extends BaseCorpus implements CorpusWrapper {

    private final Corpus corpus;
    private final CorpusFilter filter;
    private final CorpusNormalizer normalizer;

    public FilteredCorpus(Corpus corpus, CorpusFilter filter, CorpusNormalizer normalizer) {
        this.corpus = corpus;
        this.filter = filter;
        this.normalizer = normalizer;
    }

    @Override
    public String getName() {
        return corpus.getName();
    }

    @Override
    public Language getLanguage() {
        return corpus.getLanguage();
    }

    @Override
    public LineReader getContentReader() throws IOException {
        this.initialize();

        return new LineReader() {

            private final Language language = corpus.getLanguage();
            private final LineReader reader = corpus.getContentReader();
            private int index = 0;

            @Override
            public String readLine() throws IOException {
                String next;

                while ((next = reader.readLine()) != null) {
                    if (normalizer != null)
                        next = normalizer.normalize(next);
                    boolean accept = filter.accept(language, next, index);

                    index++;

                    if (accept)
                        return next;
                }

                return null;
            }

            @Override
            public void close() throws IOException {
                reader.close();
                filter.clear();
            }
        };
    }

    private void initialize() throws IOException {
        Language language = getLanguage();
        CorpusFilter.Initializer initializer = filter.getInitializer();

        if (initializer != null) {
            initializer.onBegin();

            LineReader reader = null;
            try {
                reader = corpus.getContentReader();

                int index = 0;
                String line;

                while ((line = reader.readLine()) != null) {
                    if (normalizer != null)
                        line = normalizer.normalize(line);
                    initializer.onLine(language, line, index);
                    index++;
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }

            initializer.onEnd();
        }
    }

    @Override
    public LineWriter getContentWriter(boolean append) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getRawContentReader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Corpus getWrappedCorpus() {
        return corpus;
    }
}
