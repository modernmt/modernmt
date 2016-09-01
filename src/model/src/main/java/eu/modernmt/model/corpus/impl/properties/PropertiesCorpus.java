package eu.modernmt.model.corpus.impl.properties;

import eu.modernmt.io.DefaultCharset;
import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by davide on 05/07/16.
 */
public class PropertiesCorpus implements Corpus {

    private final String name;
    private final File file;
    private final Locale language;

    public PropertiesCorpus(String name, File file, Locale language) {
        this.name = name;
        this.file = file;
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
        return new PropertiesLineReader(file);
    }

    @Override
    public LineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getRawContentReader() throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    private static final class PropertiesLineReader implements LineReader {

        private final Iterator<Object> iterator;

        public PropertiesLineReader(File file) throws IOException {
            Reader reader = null;
            Properties properties;

            try {
                reader = new InputStreamReader(new FileInputStream(file), DefaultCharset.get());

                properties = new Properties();
                properties.load(reader);
            } finally {
                IOUtils.closeQuietly(reader);
            }

            this.iterator = properties.values().iterator();
        }

        @Override
        public String readLine() {
            return iterator.hasNext() ? iterator.next().toString() : null;
        }

        @Override
        public void close() {
            // Nothing to do
        }
    }
}
