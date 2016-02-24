package eu.modernmt.model;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;

/**
 * Created by davide on 10/07/15.
 */
public interface Corpus {

    String getName();

    Locale getLanguage();

    Reader getContentReader() throws IOException;

    Writer getContentWriter(boolean append) throws IOException;

}
