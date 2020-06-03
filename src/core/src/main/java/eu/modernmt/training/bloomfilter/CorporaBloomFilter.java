package eu.modernmt.training.bloomfilter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;

public class CorporaBloomFilter {

    private final BloomFilter<String> bloomFilter;

    public CorporaBloomFilter(long expectedEntries) {
        this(expectedEntries, 1. / 1000000.);
    }

    public CorporaBloomFilter(long expectedEntries, double fpp) {
        this.bloomFilter = BloomFilter.create(new StringFunnel(), expectedEntries, fpp);
    }

    public boolean accept(MultilingualCorpus.StringPair pair, int lengthThreshold) {
        if (lengthThreshold > 0 &&
                pair.source.length() < lengthThreshold && pair.target.length() < lengthThreshold) {
            return true;
        } else {
            synchronized (bloomFilter) {
                // This is not thread safe, even in v24
                return bloomFilter.put(CorporaBloomFilter.StringFunnel.toString(pair));
            }
        }
    }

    public boolean accept(String line, int lengthThreshold) {
        if (lengthThreshold > 0 && line.length() < lengthThreshold) {
            return true;
        } else {
            synchronized (CorporaBloomFilter.this) {
                // This is not thread safe, even in v24
                return bloomFilter.put(line);
            }
        }
    }

    public MultilingualCorpus wrap(final MultilingualCorpus corpus, final int lengthThreshold) {
        return new BloomFilterMultilingualCorpus(this, corpus, lengthThreshold);
    }

    public Corpus wrap(final Corpus corpus, final int lengthThreshold) {
        return new BloomFilterCorpus(this, corpus, lengthThreshold);
    }

    static final class StringFunnel implements Funnel<String> {

        public static String toString(MultilingualCorpus.StringPair pair) {
            return pair.language.toString() + '\n' +
                    pair.source.replace('\n', ' ') + '\n' +
                    pair.target.replace('\n', ' ');
        }

        @Override
        public void funnel(String from, PrimitiveSink into) {
            into.putString(from, UTF8Charset.get());
        }

    }
}
