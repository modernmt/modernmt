package eu.modernmt.model.corpus;

import java.io.Closeable;
import java.io.IOException;

public interface TUReader extends Closeable {

    TranslationUnit read() throws IOException;

}
