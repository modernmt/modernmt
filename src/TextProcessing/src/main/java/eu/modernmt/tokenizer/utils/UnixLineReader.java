package eu.modernmt.tokenizer.utils;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 11/01/16.
 */
public class UnixLineReader extends Reader {

    private Reader reader;
    private char[] buffer;
    private int nextChar = 0;
    private int bufferLen = 0;

    private static int defaultCharBufferSize = 8192;
    private static int defaultExpectedLineLength = 80;

    public UnixLineReader(Reader reader) {
        this.reader = reader;
        this.buffer = new char[defaultCharBufferSize];
    }

    private boolean fillFromBuffer(StringBuffer s) {
        boolean stop = false;
        int offset = nextChar;
        int len = 0;

        boolean lastWasCarriageReturn = false;

        for (; nextChar < bufferLen; nextChar++) {
            if (buffer[nextChar] == '\n') {
                stop = true;
                nextChar++;
                if (lastWasCarriageReturn) len--;
                break;
            } else {
                lastWasCarriageReturn = buffer[nextChar] == '\r';
                len++;
            }
        }

        if (len > 0)
            s.append(buffer, offset, len);

        return stop;
    }

    public String readLine() throws IOException {
        if (bufferLen < 0)
            return null;

        StringBuffer s = new StringBuffer(defaultExpectedLineLength);

        for (; ; ) {
            boolean stop = fillFromBuffer(s);
            if (stop) break;

            bufferLen = reader.read(buffer, 0, buffer.length);
            nextChar = 0;
            if (bufferLen < 0)
                return s.length() > 0 ? s.toString() : null;
        }

        return s.toString();
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

}
