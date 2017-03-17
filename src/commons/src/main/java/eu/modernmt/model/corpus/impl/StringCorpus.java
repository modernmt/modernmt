package eu.modernmt.model.corpus.impl;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.io.UnixLineWriter;
import eu.modernmt.model.corpus.Corpus;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

/**
 * Created by davide on 24/02/16.
 */
public class StringCorpus implements Corpus {

    private final String name;
    private final Locale language;
    private String content;

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
    public LineReader getContentReader() throws IOException {
        return new UnixLineReader(new StringReader(content));
    }

    @Override
    public LineWriter getContentWriter(boolean append) throws IOException {
        return new StringLineWriter(append);
    }

    @Override
    public Reader getRawContentReader() throws IOException {
        return new StringReader(content);
    }

    @Override
    public String toString() {
        return content;
    }

    private class StringLineWriter extends UnixLineWriter {

        private final boolean append;

        private StringLineWriter(boolean append) {
            super(new StringWriter());
            this.append = append;
        }

        @Override
        public void close() throws IOException {
            super.close();
            StringCorpus.this.content = (append ? StringCorpus.this.content : "") + super.writer.toString();
        }
    }

}
