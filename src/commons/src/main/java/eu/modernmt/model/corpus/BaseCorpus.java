package eu.modernmt.model.corpus;

import eu.modernmt.io.LineReader;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Created by davide on 31/07/17.
 */
public abstract class BaseCorpus implements Corpus {

    private int lineCount = -1;

    @Override
    public int getLineCount() {
        if (lineCount < 0) {
            synchronized (this) {
                if (lineCount < 0)
                    try {
                        lineCount = countLines();
                    } catch (IOException e) {
                        lineCount = 0;
                    }
            }
        }

        return lineCount;
    }

    private int countLines() throws IOException {
        int count = 0;

        LineReader reader = null;

        try {
            reader = getContentReader();
            while (reader.readLine() != null) count++;
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return count;
    }

}
