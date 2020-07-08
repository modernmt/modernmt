package eu.modernmt.training.bloomfilter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.TranslationUnit;

public class CorporaBloomFilter {

    private final BloomFilter<String> bloomFilter;

    public CorporaBloomFilter(long expectedEntries) {
        this(expectedEntries, 1. / 1000000.);
    }

    public CorporaBloomFilter(long expectedEntries, double fpp) {
        this.bloomFilter = BloomFilter.create(new StringFunnel(), expectedEntries, fpp);
    }

    public boolean put(TranslationUnit tu) {
        synchronized (bloomFilter) {
            // This is not thread safe, even in v24
            return bloomFilter.put(CorporaBloomFilter.StringFunnel.toString(tu));
        }
    }

    public boolean put(String line) {
        synchronized (bloomFilter) {
            // This is not thread safe, even in v24
            return bloomFilter.put(line);
        }
    }

    public boolean contains(TranslationUnit tu) {
        synchronized (bloomFilter) {
            // This is not thread safe, even in v24
            return bloomFilter.mightContain(CorporaBloomFilter.StringFunnel.toString(tu));
        }
    }

    public boolean contains(String line) {
        synchronized (bloomFilter) {
            // This is not thread safe, even in v24
            return bloomFilter.mightContain(line);
        }
    }

    boolean accept(TranslationUnit tu, int lengthThreshold) {
        if (lengthThreshold > 0 &&
                tu.source.length() < lengthThreshold && tu.target.length() < lengthThreshold) {
            return true;
        } else {
            return put(tu);
        }
    }

    boolean accept(String line, int lengthThreshold) {
        if (lengthThreshold > 0 && line.length() < lengthThreshold) {
            return true;
        } else {
            return put(line);
        }
    }

    public MultilingualCorpus wrap(final MultilingualCorpus corpus, final int lengthThreshold) {
        return new BloomFilterMultilingualCorpus(this, corpus, lengthThreshold);
    }

    public Corpus wrap(final Corpus corpus, final int lengthThreshold) {
        return new BloomFilterCorpus(this, corpus, lengthThreshold);
    }

    static final class StringFunnel implements Funnel<String> {

        public static String toString(TranslationUnit tu) {
            return tu.language.toString() + '\n' +
                    tu.source.replace('\n', ' ') + '\n' +
                    tu.target.replace('\n', ' ');
        }

        @Override
        public void funnel(String from, PrimitiveSink into) {
            into.putString(from, UTF8Charset.get());
        }

    }
}
