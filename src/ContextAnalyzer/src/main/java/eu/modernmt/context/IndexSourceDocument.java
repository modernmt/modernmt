package eu.modernmt.context;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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

    static IndexSourceDocument fromReader(final Reader content, final Locale lang) {
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
                return content;
            }
        };
    }

    String getName();

    Locale getLanguage();

    Reader getContentReader() throws IOException;

}
