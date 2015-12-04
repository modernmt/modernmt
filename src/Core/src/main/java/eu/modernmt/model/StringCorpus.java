package eu.modernmt.model;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Created by davide on 18/09/15.
 */
public class StringCorpus implements Corpus {

    private String name;
    private String language;
    private String content;

    public StringCorpus(String name, String language, String content) {
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
