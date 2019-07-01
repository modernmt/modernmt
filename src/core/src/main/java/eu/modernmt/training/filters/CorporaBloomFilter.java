package eu.modernmt.training.filters;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.Language2;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.io.Reader;

public class CorporaBloomFilter {

    private final BloomFilter<String> bloomFilter;

    public CorporaBloomFilter(long expectedEntries) {
        this(expectedEntries, 1. / 1000000.);
    }

    public CorporaBloomFilter(long expectedEntries, double fpp) {
        this.bloomFilter = BloomFilter.create(new StringFunnel(), expectedEntries, fpp);
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
                                // This is not thread safe, even in v24
                                write = bloomFilter.put(StringFunnel.toString(pair));
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

    public Corpus wrap(final Corpus corpus, final int lengthThreshold) {
        return new Corpus() {

            @Override
            public String getName() {
                return corpus.getName();
            }

            @Override
            public Language2 getLanguage() {
                return corpus.getLanguage();
            }

            @Override
            public LineReader getContentReader() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Reader getRawContentReader() {
                throw new UnsupportedOperationException();
            }

            @Override
            public LineWriter getContentWriter(boolean append) throws IOException {
                return new LineWriter() {

                    private final LineWriter writer = corpus.getContentWriter(append);

                    @Override
                    public void writeLine(String line) throws IOException {
                        boolean write;

                        if (lengthThreshold > 0 && line.length() < lengthThreshold) {
                            write = true;
                        } else {
                            synchronized (CorporaBloomFilter.this) {
                                // This is not thread safe, even in v24
                                write = bloomFilter.put(line);
                            }
                        }

                        if (write)
                            writer.writeLine(line);
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

    private static final class StringFunnel implements Funnel<String> {

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
