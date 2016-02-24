package eu.modernmt.model.impl;

import eu.modernmt.model.Corpus;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Locale;

/**
 * Created by davide on 24/02/16.
 */
public class StringCorpus implements Corpus {

    private final String name;
    private final Locale language;
    private final String content;

    public StringCorpus(String name, Locale language, String content) {
        this.name = name;
        this.language = language;
        this.content = content;
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
        return new StringReader(content);
    }

    @Override
    public Writer getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

}
