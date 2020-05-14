package eu.modernmt.training.bloomfilter;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.BaseCorpus;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.CorpusWrapper;

import java.io.IOException;
import java.io.Reader;

public class BloomFilterCorpus extends BaseCorpus implements CorpusWrapper {

    private final CorporaBloomFilter bloomFilter;
    private final Corpus corpus;
    private final int lengthThreshold;

    public BloomFilterCorpus(CorporaBloomFilter bloomFilter, Corpus corpus, int lengthThreshold) {
        this.bloomFilter = bloomFilter;
        this.corpus = corpus;
        this.lengthThreshold = lengthThreshold;
    }

    @Override
    public Corpus getWrappedCorpus() {
        return corpus;
    }

    @Override
    public String getName() {
        return corpus.getName();
    }

    @Override
    public Language getLanguage() {
        return corpus.getLanguage();
    }

    @Override
    public Reader getRawContentReader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LineReader getContentReader() throws IOException {
        return new LineReader() {

            private final LineReader reader = corpus.getContentReader();

            @Override
            public String readLine() throws IOException {
                String line = reader.readLine();
                while (line != null && !bloomFilter.accept(line, lengthThreshold))
                    line = reader.readLine();

                return line;
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };
    }

    @Override
    public LineWriter getContentWriter(boolean append) throws IOException {
        return new LineWriter() {

            private final LineWriter writer = corpus.getContentWriter(append);

            @Override
            public void writeLine(String line) throws IOException {
                if (bloomFilter.accept(line, lengthThreshold))
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

}
