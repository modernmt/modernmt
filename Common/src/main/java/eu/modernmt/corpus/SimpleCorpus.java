package eu.modernmt.corpus;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Created by davide on 18/09/15.
 */
public class SimpleCorpus implements Corpus {

    private String name;
    private String language;
    private String content;

    public SimpleCorpus(String name, String language, String content) {
        this.name = name;
        this.language = language;
        this.content = content;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public Reader getContentReader() throws IOException {
        return new StringReader(content);
    }
}
