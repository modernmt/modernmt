package eu.modernmt.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 24/02/16.
 */
public interface BilingualCorpus {

    String getName();

    Locale getSourceLanguage();

    Locale getTargetLanguage();

    int getLineCount() throws IOException;

    BilingualStringReader getContentReader() throws IOException;

    BilingualStringWriter getContentWriter(boolean append) throws IOException;

    Corpus getSourceCorpus();

    Corpus getTargetCorpus();

    interface BilingualStringReader extends Closeable {

        class StringPair {

            public final String source;
            public final String target;

            public StringPair(String source, String target) {
                this.source = source;
                this.target = target;
            }
        }

        StringPair read() throws IOException;

    }

    interface BilingualStringWriter extends Closeable {

        void write(String source, String target) throws IOException;

    }

}
