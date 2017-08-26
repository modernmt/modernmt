package eu.modernmt.cleaning;

import eu.modernmt.cleaning.filters.RareNgramFilter;
import eu.modernmt.cleaning.filters.SentenceLengthFilter;
import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by davide on 14/03/16.
 */
public class FilteredMultilingualCorpus extends BaseMultilingualCorpus {

    private MultilingualCorpus corpus;
    private ArrayList<MultilingualCorpusFilter> filters;
    private ArrayList<MultilingualCorpusNormalizer> normalizers;

    public FilteredMultilingualCorpus(MultilingualCorpus corpus) {
        this.corpus = corpus;
        this.filters = new ArrayList<>(10);
        this.normalizers = new ArrayList<>(10);
    }

    public void addFilter(MultilingualCorpusFilter filter) {
        this.filters.add(filter);
    }

    public void addNormalizer(MultilingualCorpusNormalizer normalizer) {
        this.normalizers.add(normalizer);
    }

    @Override
    public MultilingualLineReader getContentReader() throws IOException {
        this.initialize();

        return new MultilingualLineReader() {

            private final MultilingualLineReader reader = corpus.getContentReader();
            private int index = 0;

            @Override
            public StringPair read() throws IOException {
                StringPair next;

                while ((next = reader.read()) != null) {
                    for (MultilingualCorpusNormalizer normalizer : normalizers)
                        normalizer.normalize(next, index);

                    boolean accept = true;

                    for (MultilingualCorpusFilter filter : filters) {
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

                for (MultilingualCorpusFilter filter : filters)
                    filter.clear();
            }
        };
    }

    private void initialize() throws IOException {
        ArrayList<MultilingualCorpusFilter.FilterInitializer> initializers = new ArrayList<>(filters.size());
        for (MultilingualCorpusFilter filter : filters) {
            MultilingualCorpusFilter.FilterInitializer initializer = filter.getInitializer();
            if (initializer != null)
                initializers.add(initializer);
        }

        if (initializers.size() > 0) {
            for (MultilingualCorpusFilter.FilterInitializer initializer : initializers)
                initializer.onBegin();

            MultilingualLineReader reader = null;

            try {
                reader = corpus.getContentReader();

                int index = 0;
                StringPair pair;

                while ((pair = reader.read()) != null) {
                    for (MultilingualCorpusNormalizer normalizer : normalizers)
                        normalizer.normalize(pair, index);

                    for (MultilingualCorpusFilter.FilterInitializer initializer : initializers)
                        initializer.onPair(corpus, pair, index);

                    index++;
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }

            for (MultilingualCorpusFilter.FilterInitializer initializer : initializers)
                initializer.onEnd();
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
