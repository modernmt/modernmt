package eu.modernmt.engine;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Sentence;
import eu.modernmt.decoder.Translation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.decoder.moses.MosesFeature;
import eu.modernmt.engine.tasks.GetFeatureWeightsTask;
import eu.modernmt.engine.tasks.TranslationTask;
import eu.modernmt.network.cluster.Cluster;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.network.cluster.DistributedTask;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingServer;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 09/12/15.
 */
public class MMTServer extends Cluster {

    public static final byte SIGNAL_RESET = 0x01;
    public static final byte REQUEST_SYNC_PATH = 0x03;

    private TranslationEngine engine;
    private ContextAnalyzer contextAnalyzer;
    private HashMap<Long, TranslationSession> sessions;

    public MMTServer(TranslationEngine engine, int[] ports) throws IOException {
        super(new ZMQMessagingServer(ports[0], ports[1]));
        this.engine = engine;
        this.sessions = new HashMap<>();
    }

    @Override
    public void start() throws IOException {
        super.start();
        sendBroadcastSignal(SIGNAL_RESET, null);
        logger.info("MMT Cluster Server startup.");
    }

    @Override
    public void shutdown() {
        super.shutdown();
        logger.info("MMT Cluster Server shutdown.");
    }

    @Override
    public List<DistributedTask<?>> shutdownNow() {
        List<DistributedTask<?>> tasks = super.shutdownNow();
        logger.info("MMT Cluster Server forced shutdown.");
        return tasks;
    }

    @Override
    protected byte[] onCustomRequestReceived(byte signal, byte[] payload, int offset, int length) {
        switch (signal) {
            case REQUEST_SYNC_PATH:
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                stream.write(signal);

                String enginePath = engine.getPath().getAbsolutePath();
                try {
                    stream.write(enginePath.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new Error("UTF-8 not supported", e);
                } catch (IOException e) {
                    throw new Error("This could not happen", e);
                }

                return stream.toByteArray();
            default:
                logger.warn("Unknown request received: " + Integer.toHexString(signal));
                return new byte[0];
        }
    }

    // =============================
    //  Decoder Weights
    // =============================

    public Map<MosesFeature, float[]> getFeatureWeights() throws IOException, InterruptedException {
        GetFeatureWeightsTask task = new GetFeatureWeightsTask();
        return execute(task);
    }

    public void setFeatureWeights(Map<String, float[]> weights) throws IOException {
        engine.setDecoderWeights(weights);
        sendBroadcastSignal(SIGNAL_RESET, null);
    }

    // =============================
    //  Context Analysis
    // =============================

    private ContextAnalyzer getContextAnalyzer() throws IOException {
        if (contextAnalyzer == null) {
            contextAnalyzer = new ContextAnalyzer(engine.getContextAnalyzerIndexPath());
        }

        return contextAnalyzer;
    }

    public List<ContextDocument> getContext(File context, int limit) throws IOException {
        ContextAnalyzer analyzer = getContextAnalyzer();
        return analyzer.getContext(context, engine.getSourceLanguage(), limit);
    }

    public List<ContextDocument> getContext(String context, int limit) throws IOException {
        ContextAnalyzer analyzer = getContextAnalyzer();
        return analyzer.getContext(context, engine.getSourceLanguage(), limit);
    }

    // =============================
    //  Translation session
    // =============================

    public TranslationSession createTranslationSession(List<ContextDocument> context) throws IOException {
        DistributedTranslationSession session = new DistributedTranslationSession(context, this);
        sessions.put(session.getId(), session);
        return session;
    }

    public TranslationSession closeTranslationSession(long id) throws IOException {
        TranslationSession session = sessions.remove(id);

        if (session != null)
            session.close();

        return session;
    }

    // =============================
    //  Translate
    // =============================

    public Translation translate(String sentence, boolean textProcessing) throws IOException, InterruptedException {
        return translate(sentence, textProcessing, 0);
    }

    public Translation translate(String sentence, long sessionId, boolean textProcessing) throws IOException, InterruptedException {
        return translate(sentence, sessionId, textProcessing, 0);
    }

    public Translation translate(String sentence, List<ContextDocument> translationContext, boolean textProcessing) throws IOException, InterruptedException {
        return translate(sentence, translationContext, textProcessing, 0);
    }

    public Translation translate(String sentence, boolean textProcessing, int nbest) throws IOException, InterruptedException {
        return execute(new TranslationTask(new Sentence(sentence), textProcessing, nbest));
    }

    public Translation translate(String sentence, long sessionId, boolean textProcessing, int nbest) throws IOException, InterruptedException {
        TranslationSession session = sessions.get(sessionId);
        if (session == null)
            throw new IllegalArgumentException("Invalid session id " + sessionId);

        return execute(new TranslationTask(new Sentence(sentence), session, textProcessing, nbest));
    }

    public Translation translate(String sentence, List<ContextDocument> translationContext, boolean textProcessing, int nbest) throws IOException, InterruptedException {
        return execute(new TranslationTask(new Sentence(sentence), translationContext, textProcessing, nbest));
    }

    private <R extends Serializable> R execute(DistributedCallable<R> task) throws IOException, InterruptedException {
        try {
            return this.submit(task).get();
        } catch (ExecutionException e) {
            logger.warn("Task execution failed with exception", e.getCause());

            Throwable cause = e.getCause();
            if (cause instanceof IOException)
                throw (IOException) cause;
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new RuntimeException("Unexpected exception", cause);
        }
    }

}
