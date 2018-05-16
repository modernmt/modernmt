package eu.modernmt.xml;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XMLFixInputStreamReader wraps an InputStream and filter out invalid XML entities.
 * This is necessary because some tools, for example SDL Trados, creates invalid XML
 * with entities such as &#x1E; &#x1F; etc...
 * This implementation replaces those entities with whitespaces.
 */
public class XMLFixInputStreamReader extends Reader {

    private static final int BUFFER_PADDING = 10;

    private static final Pattern CHAR_ENTITY_REGEX = Pattern.compile("&#x[0-9A-Fa-f]+;");

    private final InputStreamReader reader;

    private char[] buffer = null;
    private int bufferOffset = 0;
    private int bufferEnd = 0;
    private int bufferLength = 0;

    public XMLFixInputStreamReader(InputStream in) {
        super(in);
        reader = new InputStreamReader(in);
    }

    public XMLFixInputStreamReader(InputStream in, String charsetName)
            throws UnsupportedEncodingException {
        super(in);
        reader = new InputStreamReader(in, charsetName);
    }

    public XMLFixInputStreamReader(InputStream in, Charset cs) {
        super(in);
        reader = new InputStreamReader(in, cs);
    }

    @Override
    public boolean ready() throws IOException {
        return reader.ready();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Override
    public int read(char cbuf[], int offset, int length) throws IOException {
        if (bufferOffset == bufferEnd) {
            if (bufferLength == -1)
                return -1;

            this.load(length);
        }

        if (bufferOffset < bufferEnd) {
            int read = Math.min(bufferEnd - bufferOffset, length);
            System.arraycopy(buffer, bufferOffset, cbuf, offset, read);
            bufferOffset += read;

            return read;
        } else {
            return bufferLength == -1 ? -1 : 0;
        }
    }

    private int fill(char[] buffer, int offset, int length) throws IOException {
        int read = 0;

        while (true) {
            int result = reader.read(buffer, offset + read, length - read);
            if (result == -1)
                break;

            escapeControlChars(buffer, offset + read, result);

            read += result;
            if (read == length)
                break;
        }

        return read;
    }

    private void load(int length) throws IOException {
        int tailLength = this.bufferLength - this.bufferEnd;
        int capacity = length + BUFFER_PADDING;

        char[] _buffer = (buffer == null || buffer.length < capacity) ? new char[capacity] : buffer;
        if (buffer != null && tailLength > 0)
            System.arraycopy(buffer, bufferEnd, _buffer, 0, tailLength);
        buffer = _buffer;

        int read = this.fill(buffer, tailLength, capacity - tailLength);
        if (read < capacity - tailLength) {
            bufferEnd = read + tailLength;
            bufferLength = -1;
        } else {
            bufferEnd = length;
            bufferLength = capacity;
        }
        bufferOffset = 0;

        CharBuffer sequence = CharBuffer.wrap(buffer, 0, Math.max(bufferLength, bufferEnd));

        Matcher m = CHAR_ENTITY_REGEX.matcher(sequence);
        while (m.find()) {
            int matchStart = m.start();
            int matchLength = m.end() - matchStart;

            int c = Integer.parseInt(new String(buffer, matchStart + 3, matchLength - 4), 16);

            if (!accept(c)) {
                if (matchLength == 5) { // 1 digit
                    buffer[matchStart + 3] = '9';
                } else {
                    for (int i = matchStart + 3; i < matchStart + matchLength - 2; i++)
                        buffer[i] = '0';
                    buffer[matchStart + matchLength - 2] = '0';
                    buffer[matchStart + matchLength - 3] = '2';
                }
            }
        }
    }

    private static boolean accept(int c) {
        // https://www.w3.org/TR/REC-xml/#charsets
        return (c == 0x09) || (c == 0x0A) || (c == 0x0D) ||
                (0x20 <= c && c <= 0xD7FF) ||
                (0xE000 <= c && c <= 0xFFFD) ||
                (0x10000 <= c && c <= 0x10FFFF);
    }

    private static void escapeControlChars(char[] buffer, int offset, int length) {
        for (int i = 0; i < length; i++) {
            char c = buffer[offset + i];

            if (!accept(c))
                buffer[offset + i] = ' ';
        }
    }

}
