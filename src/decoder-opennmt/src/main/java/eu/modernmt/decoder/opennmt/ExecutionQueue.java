package eu.modernmt.decoder.opennmt;

import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.opennmt.model.TranslationRequest;
import eu.modernmt.decoder.opennmt.model.TranslationResponse;
import eu.modernmt.model.Sentence;

import java.io.*;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 22/05/17.
 */
class ExecutionQueue implements Closeable {

    private static final TranslationResponse POISON_PILL = new TranslationResponse(0L, null, null);

    private final Random rand = new Random();
    private final Process decoder;
    private final OutputStream stdin;
    private final ConcurrentHashMap<Long, BlockingQueue<TranslationResponse>> pendingRequests;
    private final ResponseCollector collector;
    private boolean isClosed;

    public ExecutionQueue(Process decoder) {
        this.decoder = decoder;
        this.collector = new ResponseCollector(decoder.getInputStream());
        this.stdin = decoder.getOutputStream();
        this.pendingRequests = new ConcurrentHashMap<>();

        this.collector.start();
    }

    public PendingTranslation execute(TranslationRequest request) throws OpenNMTException {
        this.collector.checkError();

        SynchronousQueue<TranslationResponse> handoff = new SynchronousQueue<>();

        long id;
        do {
            id = rand.nextLong();
        } while (enqueue(id, handoff) != null);

        request.setId(id);

        String payload = request.toJSON();

        try {
            this.stdin.write(payload.getBytes("UTF-8"));
            this.stdin.write('\n');
            this.stdin.flush();
        } catch (IOException e) {
            throw new OpenNMTException("Unable to communicate with OpenNMT process.", e);
        }

        return new PendingTranslation(request, handoff);
    }

    private synchronized BlockingQueue<TranslationResponse> enqueue(long id, SynchronousQueue<TranslationResponse> handoff)
            throws OpenNMTRejectedExecutionException {
        if (isClosed)
            throw new OpenNMTRejectedExecutionException();
        return this.pendingRequests.putIfAbsent(id, handoff);
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            isClosed = true;
        }

        for (BlockingQueue<TranslationResponse> queue : pendingRequests.values())
            queue.offer(POISON_PILL);

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

    class PendingTranslation {

        private static final int TIMEOUT_IN_SECONDS = 60;

        private final long id;
        private final Sentence source;
        private final SynchronousQueue<TranslationResponse> handoff;

        PendingTranslation(TranslationRequest request, SynchronousQueue<TranslationResponse> handoff) {
            this.id = request.getId();
            this.source = request.getSentence();
            this.handoff = handoff;
        }

        DecoderTranslation get() throws OpenNMTException {
            try {
                TranslationResponse response = handoff.poll(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
                if (response == POISON_PILL)
                    throw new OpenNMTRejectedExecutionException();

                if (response == null)
                    throw new OpenNMTTimeoutException();

                if (response.hasException())
                    throw response.getException();

                return response.getTranslation(source);
            } catch (InterruptedException e) {
                throw new OpenNMTRejectedExecutionException(e);
            } finally {
                pendingRequests.remove(id);
            }
        }

    }

    private class ResponseCollector extends Thread {

        private final BufferedReader reader;
        private OpenNMTException exception;

        public ResponseCollector(InputStream input) {
            this.reader = new BufferedReader(new InputStreamReader(input));
            this.exception = null;
        }

        private String readLine() {
            try {
                return reader.readLine();
            } catch (IOException e) {
                return null; // Process died
            }
        }

        public void checkError() throws OpenNMTException {
            if (this.exception != null)
                throw this.exception;
        }

        @Override
        public void run() {
            String line;

            while ((line = readLine()) != null) {
                TranslationResponse response;

                try {
                    response = TranslationResponse.fromJSON(line);
                } catch (RuntimeException e) {
                    this.exception = new OpenNMTException("Invalid response from OpenNMT: " + line, e);
                    break;
                }

                long id = response.getId();

                BlockingQueue<TranslationResponse> handoff = pendingRequests.get(id);
                if (handoff != null)
                    handoff.offer(response);
            }
        }
    }
}
