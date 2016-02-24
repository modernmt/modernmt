package eu.modernmt.processing.framework;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/**
 * Created by davide on 26/01/16.
 */
public interface PipelineOutputStream<V> extends Closeable {

    void write(V value) throws IOException;

    static PipelineOutputStream<String> fromWriter(final Writer writer) {
        return new PipelineOutputStream<String>() {
            @Override
            public void write(String value) throws IOException {
                writer.write(value);
                writer.write('\n');
            }

            @Override
            public void close() throws IOException {
                writer.close();
            }
        };
    }

}
