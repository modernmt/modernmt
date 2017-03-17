package eu.modernmt.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by davide on 04/07/16.
 */
public interface LineReader extends Closeable {

    String readLine() throws IOException;

}
