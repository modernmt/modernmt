package eu.modernmt.model.impl.tmx;

import eu.modernmt.model.Corpus;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
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
    public Reader getContentReader() throws IOException {
        // If needed, we should choose between wrap a LineReader into a Reader,
        // or change the method signature to "public LineReader getContentReader()"
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }
}
