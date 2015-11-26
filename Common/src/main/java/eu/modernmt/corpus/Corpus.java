package eu.modernmt.corpus;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 10/07/15.
 */
public interface Corpus {

    public String getName();

    public String getLanguage();

    public Reader getContentReader() throws IOException;

}
