package eu.modernmt.api.framework;

import eu.modernmt.io.FileProxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

/**
 * Created by davide on 04/10/17.
 */
public class ParameterFileProxy implements FileProxy {

    private final FileParameter file;
    private final boolean gzipped;

    public ParameterFileProxy(FileParameter file, boolean gzipped) {
        this.file = file;
        this.gzipped = gzipped;
    }

    @Override
    public String getFilename() {
        return file.getFilename();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream stream = file.getInputStream();
        if (gzipped)
            stream = new GZIPInputStream(stream);

        return stream;
    }

    @Override
    public OutputStream getOutputStream(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return getFilename();
    }
}
