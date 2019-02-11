package eu.modernmt.decoder.neural.execution;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import eu.modernmt.io.UTF8Charset;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public abstract class PythonProcess implements Closeable {

    private static final Map<String, Level> LOG_LEVELS = ImmutableMap.of(
            "CRITICAL", Level.FATAL,
            "ERROR", Level.ERROR,
            "WARNING", Level.WARN,
            "INFO", Level.INFO,
            "DEBUG", Level.DEBUG
    );

    public static String getNativeLogLevel() {
        Logger logger = LogManager.getLogger(PythonProcess.class);
        Level level = logger.getLevel();

        for (Map.Entry<String, Level> entry : LOG_LEVELS.entrySet()) {
            if (level.equals(entry.getValue()))
                return entry.getKey().toLowerCase();
        }

        return null;
    }

    protected final Logger logger = LogManager.getLogger(getClass());

    private final Process process;
    private OutputStream stdin = null;
    private StdoutThread stdoutThread = null;
    private StreamPollingThread logThread = null;

    protected PythonProcess(Process process) {
        this.process = process;
    }

    protected void connectStdin(OutputStream stdin) {
        this.stdin = stdin;
    }

    protected void connectStdout(InputStream stdout) {
        this.stdoutThread = new StdoutThread(stdout);
        this.stdoutThread.start();
    }

    protected void connectStderr(InputStream stderr) {
        this.logThread = new LogThread(stderr);
        this.logThread.start();
    }

    protected void connect() {
        connectStdin(process.getOutputStream());
        connectStderr(process.getErrorStream());
        connectStdout(process.getInputStream());
    }

    protected void send(String line) throws IOException {
        this.stdin.write(line.getBytes(UTF8Charset.get()));
        this.stdin.write('\n');
        this.stdin.flush();
    }

    protected String recv() throws IOException {
        return this.stdoutThread.readLine();
    }

    protected String recv(long timeout, TimeUnit unit) throws IOException {
        return this.stdoutThread.readLine(timeout, unit);
    }

    protected boolean isAlive() {
        return process == null || this.process.isAlive();
    }

    protected int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    protected boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return process.waitFor(timeout, unit);
    }

    @Override
    public void close() {
        if (logThread != null)
            logThread.interrupt();
        if (stdoutThread != null)
            stdoutThread.interrupt();

        IOUtils.closeQuietly(stdin);

        if (process != null) {
            process.destroy();

            try {
                process.waitFor(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Nothing to do
            }

            if (process.isAlive())
                process.destroyForcibly();

            try {
                process.waitFor();
            } catch (InterruptedException e) {
                // Nothing to do
            }
        }

        if (logThread != null) {
            try {
                logThread.join();
            } catch (InterruptedException e) {
                // Ignore it
            }
        }

        if (stdoutThread != null) {
            try {
                stdoutThread.join();
            } catch (InterruptedException e) {
                // ignore it
            }
        }
    }

    private class LogThread extends StreamPollingThread {

        private final JsonParser parser = new JsonParser();

        public LogThread(InputStream stdout) {
            super(stdout);
        }

        @Override
        protected void onLineRead(String line) {
            if (line == null)
                return;

            try {
                JsonObject json = parser.parse(line).getAsJsonObject();

                String strLevel = json.get("level").getAsString();
                String message = json.get("message").getAsString();
                String loggerName = json.get("logger").getAsString();

                Level level = LOG_LEVELS.getOrDefault(strLevel, Level.DEBUG);
                logger.log(level, "(" + loggerName + ") " + message);
            } catch (JsonSyntaxException e) {
                logger.warn("Unable to parse python log entry: " + line);
            }
        }

        @Override
        protected void onIOException(IOException e) {
            logger.error("Failed to read from neural process STDERR", e);
        }

    }

    private class StdoutThread extends StreamPollingThread {

        private final Object POISON_PILL = new Object();
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
                handoff.offer(POISON_PILL);
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

}
