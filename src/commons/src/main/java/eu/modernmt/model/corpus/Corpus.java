package eu.modernmt.model.corpus;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 10/07/15.
 */
public interface Corpus {

    String getName();

    Language getLanguage();

    int getLineCount();

    LineReader getContentReader() throws IOException;

    LineWriter getContentWriter(boolean append) throws IOException;

    Reader getRawContentReader() throws IOException, UnsupportedOperationException;

}
