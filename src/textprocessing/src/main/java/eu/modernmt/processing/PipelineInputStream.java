package eu.modernmt.processing;

import eu.modernmt.io.LineReader;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by davide on 26/01/16.
 */
public interface PipelineInputStream<V> extends Closeable {

    V read() throws IOException;

    static PipelineInputStream<String> fromLineReader(final LineReader reader) {
        return new PipelineInputStream<String>() {

            @Override
            public void close() throws IOException {
                reader.close();
            }

            @Override
            public String read() throws IOException {
                return reader.readLine();
            }
        };
    }

}
