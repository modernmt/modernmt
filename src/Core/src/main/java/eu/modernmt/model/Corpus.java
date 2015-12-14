package eu.modernmt.model;

import eu.modernmt.context.IndexSourceDocument;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

/**
 * Created by davide on 10/07/15.
 */
public interface Corpus extends IndexSourceDocument {

    String getName();

    Locale getLanguage();

    Reader getContentReader() throws IOException;

}
