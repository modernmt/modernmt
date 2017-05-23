package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.OpenNMTRejectedExecutionException;
import eu.modernmt.decoder.opennmt.model.TranslationRequest;
import eu.modernmt.decoder.opennmt.model.TranslationResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 23/05/17.
 */
public class SingleThreadExecutionQueue implements ExecutionQueue {

    private final Process decoder;
    private final OutputStream stdin;
    private final BufferedReader stdout;

    public SingleThreadExecutionQueue(ProcessBuilder builder) throws OpenNMTException {
        try {
            this.decoder = builder.start();
            this.stdin = this.decoder.getOutputStream();
            this.stdout = new BufferedReader(new InputStreamReader(this.decoder.getInputStream()));
        } catch (IOException e) {
            throw new OpenNMTException("Unable to start OpenNMT process", e);
        }
    }

    @Override
    public synchronized PendingTranslation execute(final TranslationRequest request) throws OpenNMTException {
        if (!decoder.isAlive())
            throw new OpenNMTRejectedExecutionException();

        String payload = request.toJSON();

        try {
            this.stdin.write(payload.getBytes("UTF-8"));
            this.stdin.write('\n');
            this.stdin.flush();
        } catch (IOException e) {
            throw new OpenNMTException("Failed to send request to OpenNMT decoder", e);
        }

        String line;
        try {
            line = stdout.readLine();
        } catch (IOException e) {
            throw new OpenNMTException("Failed to read response from OpenNMT decoder", e);
        }

        final TranslationResponse response = TranslationResponse.fromJSON(line);

        return () -> {
            if (response.hasException())
                throw response.getException();

            return response.getTranslation(request.getSentence());
        };
    }

    @Override
    public synchronized void close() throws IOException {
        decoder.destroy();

        try {
            decoder.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Nothing to do
        }

        if (decoder.isAlive())
            decoder.destroyForcibly();

        try {
            decoder.waitFor();
        } catch (InterruptedException e) {
            // Nothing to do
        }
    }
}
