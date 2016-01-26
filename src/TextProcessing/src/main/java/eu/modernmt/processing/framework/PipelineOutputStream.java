package eu.modernmt.processing.framework;

import java.io.*;

/**
 * Created by davide on 26/01/16.
 */
public interface PipelineOutputStream<V> extends Closeable {

    void write(V value) throws IOException;

    static <T> PipelineOutputStream<T> blackHole() {
        return new PipelineOutputStream<T>() {
            @Override
            public void write(T value) throws IOException {
                // Ignore
            }

            @Override
            public void close() throws IOException {
                // Ignore
            }
        };
    }

    static PipelineOutputStream<String> fromOutputStream(OutputStream _stream) {
        return new PipelineOutputStream<String>() {

            private PrintStream wrap(OutputStream s) {
                try {
                    return s instanceof PrintStream ? (PrintStream) s : new PrintStream(s, true, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new Error("Unsupported UTF-8", e);
                }
            }

            private PrintStream stream = wrap(_stream);

            @Override
            public void close() throws IOException {
                stream.close();
            }

            @Override
            public void write(String value) throws IOException {
                stream.print(value + '\n');
            }
        };
    }

}
