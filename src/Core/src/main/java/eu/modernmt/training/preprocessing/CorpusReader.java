package eu.modernmt.training.preprocessing;

import eu.modernmt.io.LineReader;
import eu.modernmt.model.corpus.Corpus;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by davide on 22/08/16.
 */
class CorpusReader implements Closeable {

    private static final long MAX_BUFFER_LENGTH = 10000000;

    private final LineReader reader;
    private boolean drained;

    public CorpusReader(Corpus corpus) throws IOException {
        this.reader = corpus.getContentReader();
        this.drained = false;
    }

    public ArrayList<String> read(ArrayList<String> buffer) throws IOException {
        if (drained)
            return null;

        if (buffer == null)
            buffer = new ArrayList<>();

        long size = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            buffer.add(line);

            size += line.length();

            if (size >= MAX_BUFFER_LENGTH)
                return buffer;
        }

        this.drained = true;
        return buffer;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
