package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpusWrapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 14/03/16.
 */
public class FilteredMultilingualCorpus extends BaseMultilingualCorpus implements MultilingualCorpusWrapper {

    private final MultilingualCorpus corpus;
    private final FilterEngine filter;

    public FilteredMultilingualCorpus(MultilingualCorpus corpus, FilterEngine filter) {
        this.corpus = corpus;
        this.filter = filter;
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
                    filter.normalize(next, index);
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
        List<Filter.Initializer> initializers = this.filter.getInitializers();

        if (initializers.size() > 0) {
            for (Filter.Initializer initializer : initializers)
                initializer.onBegin();

            MultilingualLineReader reader = null;

            try {
                reader = corpus.getContentReader();

                int index = 0;
                StringPair pair;

                while ((pair = reader.read()) != null) {
                    filter.normalize(pair, index);

                    for (Filter.Initializer initializer : initializers)
                        initializer.onPair(corpus, pair, index);

                    index++;
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }

            for (Filter.Initializer initializer : initializers)
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

    @Override
    public MultilingualCorpus getWrappedCorpus() {
        return corpus;
    }

    @Override
    public String toString() {
        return corpus.toString();
    }
}
