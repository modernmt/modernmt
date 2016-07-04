package eu.modernmt.model;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/**
 * Created by davide on 10/07/15.
 */
public interface Corpus {

    String getName();

    Locale getLanguage();

    LineReader getContentReader() throws IOException;

    LineWriter getContentWriter(boolean append) throws IOException;

    Reader getRawContentReader() throws IOException, UnsupportedOperationException;

}
