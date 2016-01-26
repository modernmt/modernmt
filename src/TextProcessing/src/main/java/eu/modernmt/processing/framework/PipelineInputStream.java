package eu.modernmt.processing.framework;

import java.io.*;

/**
 * Created by davide on 26/01/16.
 */
public interface PipelineInputStream<V> extends Closeable {

    V read() throws IOException;

    static PipelineInputStream<String> fromInputStream(InputStream stream) {
        return fromReader(new InputStreamReader(stream));
    }

    static PipelineInputStream<String> fromReader(final Reader _reader) {
        return new PipelineInputStream<String>() {

            private UnixLineReader reader = new UnixLineReader(_reader);

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
