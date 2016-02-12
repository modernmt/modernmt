package eu.modernmt.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Locale;

/**
 * Created by davide on 28/01/16.
 */
public interface ParallelCorpus {

    String getName();

    Locale getSourceLanguage();

    Locale getTargetLanguage();

    int getLineCount() throws IOException;

    ParallelLineReader getContentReader() throws IOException;

    Reader getContentReader(Locale language) throws IOException;

    ParallelLineWriter getContentWriter(boolean append) throws IOException;

    Writer getContentWriter(Locale language, boolean append) throws IOException;

    interface ParallelLineReader extends Closeable {

        int SOURCE_LINE_INDEX = 0;
        int TARGET_LINE_INDEX = 1;

        String[] read() throws IOException;

    }

    interface ParallelLineWriter extends Closeable {

        void write(String source, String target) throws IOException;

    }

}
