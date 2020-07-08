package eu.modernmt.model.corpus;

import eu.modernmt.lang.LanguageDirection;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

/**
 * Created by davide on 24/02/16.
 */
public interface MultilingualCorpus {

    String getName();

    Set<LanguageDirection> getLanguages();

    int getLineCount(LanguageDirection language);

    TUReader getContentReader() throws IOException;

    TUWriter getContentWriter(boolean append) throws IOException;

    Corpus getCorpus(LanguageDirection language, boolean source);

}
