package eu.modernmt.model.corpus.impl.ebay4cb;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.model.corpus.Corpus;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/**
 * Created by davide on 05/07/16.
 */
public class Ebay4CBCorpus implements Corpus {

    private final String name;
    private final Locale language;
    private final File file;

    public Ebay4CBCorpus(String name, Locale language, File file) {
        this.name = name;
        this.language = language;
        this.file = file;
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
        return new Ebay4CBFileReader(file);
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
