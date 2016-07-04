package eu.modernmt.model.impl.tmx;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.model.Corpus;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
public class TMXView implements Corpus {

    // private final File tmx;
    private final String name;
    private final Locale language;

    public TMXView(File tmx, String name, Locale language) {
        // this.tmx = tmx;
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
