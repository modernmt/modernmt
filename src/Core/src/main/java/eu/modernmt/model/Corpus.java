package eu.modernmt.model;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 10/07/15.
 */
public interface Corpus {

    String getName();

    String getLanguage();

    Reader getContentReader() throws IOException;

}
