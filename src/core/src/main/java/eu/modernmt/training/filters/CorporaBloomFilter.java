package eu.modernmt.training.filters;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;

public class CorporaBloomFilter {

    private final BloomFilter<MultilingualCorpus.StringPair> bloomFilter;

    public CorporaBloomFilter(long expectedEntries) {
        this(expectedEntries, 1. / 1000000.);
    }

    public CorporaBloomFilter(long expectedEntries, double fpp) {
        this.bloomFilter = BloomFilter.create(new StringPairFunnel(), expectedEntries, fpp);
    }

    public MultilingualCorpus wrap(final MultilingualCorpus corpus) {
        return new BaseMultilingualCorpus() {

            @Override
            public String getName() {
                return corpus.getName();
            }

            @Override
            public MultilingualLineReader getContentReader() {
                throw new UnsupportedOperationException();
            }

            @Override
            public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
                return new MultilingualLineWriter() {

                    private final MultilingualLineWriter writer = corpus.getContentWriter(append);

                    @Override
                    public void write(StringPair pair) throws IOException {
                        boolean write;

                        synchronized (CorporaBloomFilter.this) {
                            write = bloomFilter.put(pair); // This is not thread safe, even in v24
                        }

                        if (write)
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

        };
    }

    private static final class StringPairFunnel implements Funnel<MultilingualCorpus.StringPair> {

        @Override
        public void funnel(MultilingualCorpus.StringPair from, PrimitiveSink into) {
            into.putString(from.language.toString(), DefaultCharset.get());
            into.putString(from.source, DefaultCharset.get());
            into.putString(from.target, DefaultCharset.get());
        }

    }
}
