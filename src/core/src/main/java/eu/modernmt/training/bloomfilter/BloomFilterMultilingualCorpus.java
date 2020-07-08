package eu.modernmt.training.bloomfilter;

import eu.modernmt.model.corpus.*;

import java.io.IOException;

public class BloomFilterMultilingualCorpus extends BaseMultilingualCorpus implements MultilingualCorpusWrapper {

    private final CorporaBloomFilter bloomFilter;
    private final MultilingualCorpus corpus;
    private final int lengthThreshold;

    public BloomFilterMultilingualCorpus(CorporaBloomFilter bloomFilter, MultilingualCorpus corpus, int lengthThreshold) {
        this.bloomFilter = bloomFilter;
        this.corpus = corpus;
        this.lengthThreshold = lengthThreshold;
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
    public TUReader getContentReader() throws IOException {
        return new TUReader() {

            private final TUReader reader = corpus.getContentReader();

            @Override
            public TranslationUnit read() throws IOException {
                TranslationUnit tu = reader.read();
                while (tu != null && !bloomFilter.accept(tu, lengthThreshold))
                    tu = reader.read();

                return tu;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };
    }

    @Override
    public TUWriter getContentWriter(boolean append) throws IOException {
        return new TUWriter() {

            private final TUWriter writer = corpus.getContentWriter(append);

            @Override
            public void write(TranslationUnit tu) throws IOException {
                if (bloomFilter.accept(tu, lengthThreshold))
                    writer.write(tu);
            }

            @Override
            public void flush() throws IOException {
                writer.flush();
            }

            @Override
            public void close() throws IOException {
                writer.close();
            }
        };
    }

}
