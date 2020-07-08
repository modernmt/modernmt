package eu.modernmt.model.corpus;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.BaseCorpus;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.CorpusWrapper;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 29/08/17.
 */
public class LazyCorpus extends BaseCorpus implements CorpusWrapper {

    private final Corpus corpus;

    public LazyCorpus(Corpus corpus) {
        this.corpus = corpus;
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
    public LineReader getContentReader() throws IOException {
        return corpus.getContentReader();
    }

    @Override
    public LineWriter getContentWriter(boolean append) {
        return new LineWriter() {

            private LineWriter writer = null;

            @Override
            public void writeLine(String line) throws IOException {
                if (writer == null)
                    writer = corpus.getContentWriter(append);
                writer.writeLine(line);
            }

            @Override
            public void flush() throws IOException {
                if (writer != null)
                    writer.flush();
            }

            @Override
            public void close() throws IOException {
                if (writer != null)
                    writer.close();
            }
        };
    }

    @Override
    public Reader getRawContentReader() throws IOException, UnsupportedOperationException {
        return corpus.getRawContentReader();
    }

    @Override
    public Corpus getWrappedCorpus() {
        return corpus;
    }
}
