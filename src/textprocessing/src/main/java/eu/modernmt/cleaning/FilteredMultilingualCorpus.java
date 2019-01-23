package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpusWrapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class FilteredMultilingualCorpus extends BaseMultilingualCorpus implements MultilingualCorpusWrapper {

    private final MultilingualCorpus corpus;
    private final MultilingualCorpusFilter filter;
    private final CorpusNormalizer normalizer;

    public FilteredMultilingualCorpus(MultilingualCorpus corpus, CorpusNormalizer normalizer, MultilingualCorpusFilter filter) {
        this.corpus = corpus;
        this.normalizer = normalizer;
        this.filter = filter;
    }

    @Override
    public MultilingualCorpus getWrappedCorpus() {
        return corpus;
    }

    @Override
    public String getName() {
        return corpus.getName();
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) {
        throw new UnsupportedOperationException();
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
                    if (normalizer != null) {
                        next.source = normalizer.normalize(next.source);
                        next.target = normalizer.normalize(next.target);
                    }

                    boolean accept = filter.accept(next, index);

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
        MultilingualCorpusFilter.Initializer initializer = filter.getInitializer();

        if (initializer != null) {
            initializer.onBegin();

            MultilingualLineReader reader = null;
            try {
                reader = corpus.getContentReader();

                int index = 0;
                StringPair pair;

                while ((pair = reader.read()) != null) {
                    if (normalizer != null) {
                        pair.source = normalizer.normalize(pair.source);
                        pair.target = normalizer.normalize(pair.target);
                    }

                    initializer.onPair(pair, index);
                    index++;
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }

            initializer.onEnd();
        }
    }

}
