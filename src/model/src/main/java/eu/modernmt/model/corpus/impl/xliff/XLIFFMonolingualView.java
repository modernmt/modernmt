package eu.modernmt.model.corpus.impl.xliff;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.model.corpus.Corpus;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
class XLIFFMonolingualView implements Corpus {

    // private final File xliff;
    private final String name;
    private final Locale language;

    XLIFFMonolingualView(File xliff, String name, Locale language) {
        // this.xliff = xliff;
        this.name = name;
        this.language = language;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    @Override
    public LineReader getContentReader() throws IOException {
        // TODO: not implemented yet
        throw new UnsupportedOperationException();
    }

    @Override
    public LineWriter getContentWriter(boolean append) throws IOException {
        // TODO: not implemented yet
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getRawContentReader() throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
