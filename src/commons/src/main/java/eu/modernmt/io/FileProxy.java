package eu.modernmt.io;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by davide on 22/12/16.
 */
public interface FileProxy {

    String getFilename();

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream(boolean append) throws IOException;

    static FileProxy wrap(File file) {
        return wrap(file, false);
    }

    static FileProxy wrap(File file, boolean gzipped) {
        return new NativeFileProxy(file, gzipped);
    }

    class NativeFileProxy implements FileProxy {

        private final File file;
        private final boolean gzipped;

        public NativeFileProxy(File file, boolean gzipped) {
            this.file = file;
            this.gzipped = gzipped;
        }

        public File getFile() {
            return file;
        }

        @Override
        public String getFilename() {
            return file.getName();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream stream = new FileInputStream(file);
            if (gzipped)
                stream = new GZIPInputStream(stream);
            return stream;
        }

        @Override
        public OutputStream getOutputStream(boolean append) throws IOException {
            OutputStream stream = new FileOutputStream(file, append);
            if (gzipped)
                stream = new GZIPOutputStream(stream);
            return stream;
        }

        @Override
        public String toString() {
            return file.toString();
        }
    }

}
