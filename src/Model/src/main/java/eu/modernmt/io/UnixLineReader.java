package eu.modernmt.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * Created by davide on 24/02/16.
 */
public class UnixLineReader implements LineReader {

    private Reader reader;
    private char[] buffer;
    private int nextChar = 0;
    private int bufferLen = 0;

    private static final int defaultCharBufferSize = 8192;
    private static final int defaultExpectedLineLength = 80;

    public UnixLineReader(InputStream stream, Charset charset) {
        this(new InputStreamReader(stream, charset));
    }

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

    @Override
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
    public void close() throws IOException {
        reader.close();
    }

}
