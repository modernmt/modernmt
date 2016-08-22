package eu.modernmt.training.preprocessing;

import eu.modernmt.io.LineWriter;
import eu.modernmt.model.corpus.Corpus;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by davide on 22/08/16.
 */
public abstract class ResultWriter {

    private static final String[][] POISON_PILL = new String[0][0];

    public final Instance forCorpus(Corpus corpus) throws IOException {
        return new Instance(corpus.getContentWriter(false));
    }

    public void flush() throws IOException {
        // Default empty implementation
    }

    protected abstract void doWrite(String[][] batch, LineWriter writer) throws IOException;

    public class Instance implements Closeable {

        private final SynchronousQueue<String[][]> queue = new SynchronousQueue<>();
        private final WriterThread writerThread = new WriterThread();
        private final LineWriter writer;

        private Instance(LineWriter writer) {
            this.writer = writer;
            this.writerThread.start();
        }

        public final void write(String[][] batch) throws IOException {
            writerThread.checkForError();

            try {
                queue.put(batch);
            } catch (InterruptedException e) {
                throw new IOException("Write thread interrupted", e);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                queue.put(POISON_PILL);
                writerThread.join();
            } catch (InterruptedException e) {
                throw new IOException("Write thread interrupted", e);
            } finally {
                writer.close();
            }

            writerThread.checkForError();
        }

        private class WriterThread extends Thread {

            private IOException error = null;

            public void checkForError() throws IOException {
                if (error != null)
                    throw error;
            }

            @Override
            public void run() {
                while (true) {
                    String[][] batch;

                    try {
                        batch = queue.take();
                        if (batch == POISON_PILL)
                            batch = null;
                    } catch (InterruptedException e) {
                        batch = null;
                    }

                    if (batch == null)
                        break;

                    if (error != null)
                        continue;

                    try {
                        doWrite(batch, writer);
                    } catch (IOException e) {
                        error = e;
                    }
                }
            }

        }
    }

}
