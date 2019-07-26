package eu.modernmt.decoder.neural.queue;

import eu.modernmt.io.UTF8Charset;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

abstract class StreamPollingThread extends Thread {

    private final BufferedReader reader;
    private boolean active = true;

    public StreamPollingThread(InputStream stdout) {
        this.reader = new BufferedReader(new InputStreamReader(stdout, UTF8Charset.get()));
    }

    @Override
    public final void run() {
        while (active) {
            try {
                try {
                    String line = reader.readLine();
                    if (line == null)
                        active = false;

                    if (!active)
                        break;

                    onLineRead(line);
                } catch (IOException e) {
                    if (!active)
                        break;

                    onIOException(e);
                }
            } catch (InterruptedException e) {
                break;
            }
        }

        IOUtils.closeQuietly(reader);

        try {
            onLineRead(null);
        } catch (InterruptedException e) {
            // Ignore it
        }
    }

    public final boolean isActive() {
        return active;
    }

    protected abstract void onLineRead(String line) throws InterruptedException;

    protected abstract void onIOException(IOException e) throws InterruptedException;

    @Override
    public void interrupt() {
        this.active = false;
    }

}
