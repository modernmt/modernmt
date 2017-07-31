package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.BaseMultilingualCorpus;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by davide on 14/03/16.
 */
public class FilteredMultilingualCorpus extends BaseMultilingualCorpus {

    private MultilingualCorpus corpus;
    private ArrayList<BilingualCorpusFilter> filters;
    private ArrayList<BilingualCorpusNormalizer> normalizers;

    public FilteredMultilingualCorpus(MultilingualCorpus corpus) {
        this.corpus = corpus;
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
    public MultilingualLineReader getContentReader() throws IOException {
        for (BilingualCorpusFilter filter : filters)
            filter.onInitStart();

        this.initialize();

        for (BilingualCorpusFilter filter : filters)
            filter.onInitEnd();

        return new MultilingualLineReader() {

            private final MultilingualLineReader reader = corpus.getContentReader();
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

                for (BilingualCorpusFilter filter : filters)
                    filter.clear();
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
            MultilingualLineReader reader = null;

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
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    public MultilingualCorpus getWrappedCorpus() {
        return corpus;
    }

}
