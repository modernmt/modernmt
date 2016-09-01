package eu.modernmt.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Created by davide on 04/07/16.
 */
public class UnixLineWriter implements LineWriter {

    protected final Writer writer;

    public UnixLineWriter(OutputStream stream, Charset charset) {
        this(new OutputStreamWriter(stream, charset));
    }

    public UnixLineWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void writeLine(String line) throws IOException {
        this.writer.write(line.replace('\n', ' '));
        this.writer.write('\n');
    }

    @Override
    public void close() throws IOException {
        this.writer.flush();
        this.writer.close();
    }
}
