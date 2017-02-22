package eu.modernmt.io;

import java.io.*;

/**
 * Created by davide on 22/12/16.
 */
public interface FileProxy {

    String getFilename();

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream(boolean append) throws IOException;

    static FileProxy wrap(final File file) {
        return new FileProxy() {
            @Override
            public String getFilename() {
                return file.getName();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(file);
            }

            @Override
            public OutputStream getOutputStream(boolean append) throws IOException {
                return new FileOutputStream(file, append);
            }

            @Override
            public String toString() {
                return file.toString();
            }
        };
    }

}
