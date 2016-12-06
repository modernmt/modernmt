package eu.modernmt.cleaning;

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
    private int lineCount;
    private ArrayList<BilingualCorpusFilter> filters;
    private ArrayList<BilingualCorpusNormalizer> normalizers;

    public FilteredBilingualCorpus(BilingualCorpus corpus) {
        this.corpus = corpus;
        this.lineCount = -1;
        this.filters = new ArrayList<>(10);
        this.normalizers = new ArrayList<>(10);
    }

    public void addFilter(BilingualCorpusFilter filter) {
        this.filters.add(filter);
    }

    public void addNormalizer(BilingualCorpusNormalizer normalizer) {
        this.normalizers.add(normalizer);
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
        for (BilingualCorpusFilter filter : filters)
            filter.onInitStart();

        this.initialize();

        for (BilingualCorpusFilter filter : filters)
            filter.onInitEnd();

        return new BilingualLineReader() {

            private final BilingualLineReader reader = corpus.getContentReader();
            private int index = 0;

            @Override
            public StringPair read() throws IOException {
                StringPair next;

                while ((next = reader.read()) != null) {
                    for (BilingualCorpusNormalizer normalizer : normalizers)
                        normalizer.normalize(next, index);

                    boolean accept = true;

                    for (BilingualCorpusFilter filter : filters) {
                        if (!filter.accept(next, index)) {
                            accept = false;
                            break;
                        }
                    }

                    index++;

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

                int index = 0;
                StringPair pair;

                while ((pair = reader.read()) != null) {
                    for (BilingualCorpusNormalizer normalizer : normalizers)
                        normalizer.normalize(pair, index);

                    for (BilingualCorpusFilter.FilterInitializer initializer : initializers)
                        initializer.onPair(corpus, pair, index);

                    index++;
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

    public BilingualCorpus getWrappedCorpus() {
        return corpus;
    }

}
