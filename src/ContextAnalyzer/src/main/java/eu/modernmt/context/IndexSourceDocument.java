package eu.modernmt.context;

import java.io.*;
import java.util.Locale;

/**
 * Created by davide on 02/12/15.
 */
public interface IndexSourceDocument {

    static IndexSourceDocument fromString(final String content, final Locale lang) {
        return new IndexSourceDocument() {
            @Override
            public String getName() {
                return this.toString();
            }

            @Override
            public Locale getLanguage() {
                return lang;
            }

            @Override
            public Reader getContentReader() throws IOException {
                return new StringReader(content);
            }
        };
    }

    static IndexSourceDocument fromFile(final File content, final Locale lang) {
        return new IndexSourceDocument() {
            @Override
            public String getName() {
                return this.toString();
            }

            @Override
            public Locale getLanguage() {
                return lang;
            }

            @Override
            public Reader getContentReader() throws IOException {
                return new FileReader(content);
            }
        };
    }

    String getName();

    Locale getLanguage();

    Reader getContentReader() throws IOException;

}
