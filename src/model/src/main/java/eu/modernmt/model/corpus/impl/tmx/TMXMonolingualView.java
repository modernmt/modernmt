package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.io.FileProxy;
import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
class TMXMonolingualView implements Corpus {

    private final FileProxy tmx;
    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private final boolean useSource;

    TMXMonolingualView(FileProxy tmx, String name, Locale sourceLanguage, Locale targetLanguage, boolean useSource) {
        this.tmx = tmx;
        this.name = name;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.useSource = useSource;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getLanguage() {
        return useSource ? sourceLanguage : targetLanguage;
    }

    @Override
    public LineReader getContentReader() throws IOException {
        return new LineReader() {

            private final TMXBilingualLineReader reader = new TMXBilingualLineReader(tmx, sourceLanguage, targetLanguage);

            @Override
            public String readLine() throws IOException {
                BilingualCorpus.StringPair pair = reader.read();
                return pair == null ? null : (useSource ? pair.source : pair.target);
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };
    }

    @Override
    public LineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getRawContentReader() throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
