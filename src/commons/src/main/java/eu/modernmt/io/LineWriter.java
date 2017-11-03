package eu.modernmt.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by davide on 04/07/16.
 */
public interface LineWriter extends Closeable {

    void writeLine(String line) throws IOException;

    void flush() throws IOException;

}
