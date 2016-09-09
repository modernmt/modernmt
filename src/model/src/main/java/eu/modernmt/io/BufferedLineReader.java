package eu.modernmt.io;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by davide on 22/08/16.
 */
public class BufferedLineReader implements LineReader {

    private static final int DEFAULT_BUFFER_LENGTH = 10000000;

    private final int bufferSize;
    private final LineReader reader;
    private boolean drained;


    public BufferedLineReader(LineReader reader) throws IOException {
        this(reader, DEFAULT_BUFFER_LENGTH);
    }

    public BufferedLineReader(LineReader reader, int bufferSize) throws IOException {
        this.reader = reader;
        this.drained = false;
        this.bufferSize = bufferSize;
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    public String[] readLines() throws IOException {
        if (drained)
            return null;

        ArrayList<String> buffer = new ArrayList<>(bufferSize / 10);

        long size = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            buffer.add(line);

            size += line.length();

            if (size >= bufferSize)
                return buffer.toArray(new String[buffer.size()]);
        }

        this.drained = true;
        return buffer.toArray(new String[buffer.size()]);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
