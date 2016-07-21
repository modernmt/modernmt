package eu.modernmt.core.training.cleaning;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
public class FilteredBilingualCorpus implements BilingualCorpus {

    private BilingualCorpus corpus;
    private boolean initialized;
    private int lineCount;
    private ArrayList<BilingualCorpusFilter> filters;

    public FilteredBilingualCorpus(BilingualCorpus corpus) {
        this.corpus = corpus;
        this.lineCount = -1;
        this.initialized = false;
        this.filters = new ArrayList<>(10);
    }

    public void addFilter(BilingualCorpusFilter filter) {
        if (initialized)
            throw new IllegalStateException("Cannot add new filter after initialization");

        this.filters.add(filter);
    }

    @Override
    public int getLineCount() throws IOException {
        if (lineCount < 0) {
            synchronized (this) {
                if (lineCount < 0) {
                    this.lineCount = BilingualCorpus.getLineCount(this);
                }
            }
        }

        return this.lineCount;
    }

    @Override
    public BilingualLineReader getContentReader() throws IOException {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    this.initialize();
                    initialized = true;
                }
            }
        }

        return new BilingualLineReader() {

            private final BilingualLineReader reader = corpus.getContentReader();

            @Override
            public StringPair read() throws IOException {
                StringPair next;

                while ((next = reader.read()) != null) {
                    boolean accept = true;

                    for (BilingualCorpusFilter filter : filters) {
                        accept &= filter.accept(next);
                    }

                    if (accept)
                        return next;
                }

                return null;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };
    }

    private void initialize() throws IOException {
        ArrayList<BilingualCorpusFilter.FilterInitializer> initializers = new ArrayList<>(filters.size());
        for (BilingualCorpusFilter filter : filters) {
            BilingualCorpusFilter.FilterInitializer initializer = filter.getInitializer();
            if (initializer != null)
                initializers.add(initializer);
        }

        if (initializers.size() > 0) {
            BilingualLineReader reader = null;

            try {
                reader = corpus.getContentReader();

                StringPair pair;
                while ((pair = reader.read()) != null) {
                    for (BilingualCorpusFilter.FilterInitializer initializer : initializers)
                        initializer.onPair(corpus, pair);
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
    }

    @Override
    public String getName() {
        return corpus.getName();
    }

    @Override
    public Locale getSourceLanguage() {
        return corpus.getSourceLanguage();
    }

    @Override
    public Locale getTargetLanguage() {
        return corpus.getTargetLanguage();
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

}
