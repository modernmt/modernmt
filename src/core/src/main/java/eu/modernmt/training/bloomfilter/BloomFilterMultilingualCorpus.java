package eu.modernmt.training.bloomfilter;

import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpusWrapper;

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
    public MultilingualLineReader getContentReader() throws IOException {
        return new MultilingualLineReader() {

            private final MultilingualLineReader reader = corpus.getContentReader();

            @Override
            public StringPair read() throws IOException {
                StringPair pair = reader.read();
                while (pair != null && !bloomFilter.accept(pair, lengthThreshold))
                    pair = reader.read();

                return pair;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new MultilingualLineWriter() {

            private final MultilingualLineWriter writer = corpus.getContentWriter(append);

            @Override
            public void write(StringPair pair) throws IOException {
                if (bloomFilter.accept(pair, lengthThreshold))
                    writer.write(pair);
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
