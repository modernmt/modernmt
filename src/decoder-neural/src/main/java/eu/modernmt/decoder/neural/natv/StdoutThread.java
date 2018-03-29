package eu.modernmt.decoder.neural.natv;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

class StdoutThread extends StreamPollingThread {

    private static final Object POISON_PILL = new Object();
    private final SynchronousQueue<Object> handoff;

    public StdoutThread(InputStream stdout) {
        super(stdout);
        this.handoff = new SynchronousQueue<>();
    }

    @Override
    protected void onIOException(IOException e) throws InterruptedException {
        handoff.put(e);
    }

    @Override
    protected void onLineRead(String line) throws InterruptedException {
        if (line == null)
            handoff.put(POISON_PILL);
        else
            handoff.put(line);
    }

    public String readLine() throws IOException {
        return readLine(0, null);
    }

    public String readLine(long timeout, TimeUnit unit) throws IOException {
        if (!super.isActive())
            return null;

        Object object;

        try {
            object = unit == null ? handoff.take() : handoff.poll(timeout, unit);
        } catch (InterruptedException e) {
            return null;
        }

        if (object == null || object == POISON_PILL)
            return null;

        if (object instanceof IOException)
            throw (IOException) object;
        else
            return (String) object;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        this.handoff.poll();
    }

}
