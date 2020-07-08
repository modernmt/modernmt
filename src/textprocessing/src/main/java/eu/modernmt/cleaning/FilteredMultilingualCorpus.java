package eu.modernmt.cleaning;

import eu.modernmt.model.corpus.*;
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
    public TUWriter getContentWriter(boolean append) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TUReader getContentReader() throws IOException {
        this.initialize();

        return new TUReader() {

            private final TUReader reader = corpus.getContentReader();
            private int index = 0;

            @Override
            public TranslationUnit read() throws IOException {
                TranslationUnit next;

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

            TUReader reader = null;
            try {
                reader = corpus.getContentReader();

                int index = 0;
                TranslationUnit tu;

                while ((tu = reader.read()) != null) {
                    if (normalizer != null) {
                        tu.source = normalizer.normalize(tu.source);
                        tu.target = normalizer.normalize(tu.target);
                    }

                    initializer.onTranslationUnit(tu, index);
                    index++;
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }

            initializer.onEnd();
        }
    }

}
