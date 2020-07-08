package eu.modernmt.model.corpus;

import java.io.Closeable;
import java.io.IOException;

public interface TUWriter extends Closeable {

    void write(TranslationUnit tu) throws IOException;

    void flush() throws IOException;
}
