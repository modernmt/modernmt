package eu.modernmt.training.filters;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import eu.modernmt.io.UTF8Charset;
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

    public MultilingualCorpus wrap(final MultilingualCorpus corpus, final int lengthThreshold) {
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

                        if (lengthThreshold > 0 &&
                                pair.source.length() < lengthThreshold && pair.target.length() < lengthThreshold) {
                            write = true;
                        } else {
                            synchronized (CorporaBloomFilter.this) {
                                write = bloomFilter.put(pair); // This is not thread safe, even in v24
                            }
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
            into.putString(from.language.toString(), UTF8Charset.get());
            into.putString(from.source, UTF8Charset.get());
            into.putString(from.target, UTF8Charset.get());
        }

    }
}
