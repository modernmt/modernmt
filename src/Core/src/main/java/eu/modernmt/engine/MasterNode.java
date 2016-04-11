package eu.modernmt.engine;

import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.config.Config;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.decoder.moses.MosesFeature;
import eu.modernmt.engine.tasks.GetFeatureWeightsTask;
import eu.modernmt.engine.tasks.InsertTagsTask;
import eu.modernmt.engine.tasks.TranslationTask;
import eu.modernmt.model.AutomaticTaggedTranslation;
import eu.modernmt.network.cluster.ClusterManager;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingServer;
import eu.modernmt.processing.framework.ProcessingException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 09/12/15.
 */
public class MasterNode extends ClusterManager {

    public static final byte SIGNAL_RESET = 0x01;
    public static final byte REQUEST_SYNC_PATH = 0x03;

    private TranslationEngine engine;
    private ContextAnalyzer contextAnalyzer;
    private HashMap<Long, TranslationSession> sessions;

    public MasterNode(TranslationEngine engine, int[] ports) throws IOException {
        super(new ZMQMessagingServer(ports[0], ports[1]));
        this.engine = engine;
        this.sessions = new HashMap<>();
        this.contextAnalyzer = new ContextAnalyzer(engine.getContextAnalyzerIndexPath());
    }

    @Override
    public void start() throws IOException {
        super.start();
        sendBroadcastSignal(SIGNAL_RESET, null);
        logger.info("Master node startup");
    }

    @Override
    public void shutdown() {
        super.shutdown();
        logger.info("Master node shutdown");
    }

    @Override
    protected byte[] onCustomRequestReceived(byte signal, byte[] payload, int offset, int length) {
        switch (signal) {
            case REQUEST_SYNC_PATH:
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                stream.write(signal);

                String enginePath = engine.getPath().getAbsolutePath();
                try {
                    stream.write(enginePath.getBytes(Config.charset.get()));
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
    //  Tag aligner
    // =============================

    public AutomaticTaggedTranslation alignTags(String sentence, String translation, boolean forceTranslation) throws TranslationException {
        InsertTagsTask task;
        try {
            task = new InsertTagsTask(sentence, translation, forceTranslation);
            return this.execute(task);
        } catch (Throwable e) {
            if (e instanceof ProcessingException)
                throw new TranslationException("Problem while processing translation", e);
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new Error("Unexpected exception: " + e.getMessage(), e);
        }
    }

    public AutomaticTaggedTranslation alignTags(String sentence, String translation, boolean forceTranslation,
                                                Symmetrisation.Strategy symmetrizationStrategy) throws TranslationException {
        InsertTagsTask task;
        try {
            task = new InsertTagsTask(sentence, translation, forceTranslation, symmetrizationStrategy);
            return this.execute(task);
        } catch (Throwable e) {
            if (e instanceof ProcessingException)
                throw new TranslationException("Problem while processing translation", e);
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new Error("Unexpected exception: " + e.getMessage(), e);
        }
    }

    // =============================
    //  Decoder Weights
    // =============================

    public Map<MosesFeature, float[]> getFeatureWeights() {
        GetFeatureWeightsTask task = new GetFeatureWeightsTask();

        try {
            return this.execute(task);
        } catch (Throwable e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new Error("Unexpected exception: " + e.getMessage(), e);
        }
    }

    public void setFeatureWeights(Map<String, float[]> weights) throws IOException {
        engine.setDecoderWeights(weights);
        sendBroadcastSignal(SIGNAL_RESET, null);
    }

    // =============================
    //  Context Analysis
    // =============================

    private ContextAnalyzer getContextAnalyzer() {
        if (contextAnalyzer == null)
            throw new IllegalStateException("ContextAnalyzer has not been initialized yet");

        return contextAnalyzer;
    }

    public List<ContextDocument> getContext(File context, int limit) throws ContextAnalyzerException {
        ContextAnalyzer analyzer = getContextAnalyzer();
        return analyzer.getContext(context, engine.getSourceLanguage(), limit);
    }

    public List<ContextDocument> getContext(String context, int limit) throws ContextAnalyzerException {
        ContextAnalyzer analyzer = getContextAnalyzer();
        return analyzer.getContext(context, engine.getSourceLanguage(), limit);
    }

    // =============================
    //  Translation session
    // =============================

    public TranslationSession createTranslationSession(List<ContextDocument> context) {
        DistributedTranslationSession session = new DistributedTranslationSession(context, this);
        sessions.put(session.getId(), session);
        return session;
    }

    public TranslationSession closeTranslationSession(long id) {
        TranslationSession session = sessions.remove(id);

        if (session != null)
            try {
                session.close();
            } catch (IOException e) {
                logger.warn("Problem while closing session " + id + ": " + e.getMessage(), e);
            }

        return session;
    }

    // =============================
    //  Translate
    // =============================

    public DecoderTranslation translate(String sentence, boolean textProcessing) throws TranslationException {
        return translate(sentence, null, 0L, textProcessing, 0);
    }

    public DecoderTranslation translate(String sentence, long sessionId, boolean textProcessing) throws TranslationException {
        return translate(sentence, null, sessionId, textProcessing, 0);
    }

    public DecoderTranslation translate(String sentence, List<ContextDocument> translationContext, boolean textProcessing) throws TranslationException {
        return translate(sentence, translationContext, 0L, textProcessing, 0);
    }

    public DecoderTranslation translate(String sentence, boolean textProcessing, int nbest) throws TranslationException {
        return translate(sentence, null, 0L, textProcessing, nbest);
    }

    public DecoderTranslation translate(String sentence, long sessionId, boolean textProcessing, int nbest) throws TranslationException {
        return translate(sentence, null, sessionId, textProcessing, nbest);
    }

    public DecoderTranslation translate(String sentence, List<ContextDocument> translationContext, boolean textProcessing, int nbest) throws TranslationException {
        return translate(sentence, translationContext, 0L, textProcessing, nbest);
    }

    private DecoderTranslation translate(String text, List<ContextDocument> translationContext, long sessionId, boolean textProcessing, int nbest) throws TranslationException {
        TranslationTask task;

        if (translationContext != null) {
            task = new TranslationTask(text, translationContext, textProcessing, nbest);
        } else if (sessionId > 0) {
            TranslationSession session = sessions.get(sessionId);
            if (session == null)
                throw new IllegalArgumentException("Invalid session id " + sessionId);

            task = new TranslationTask(text, session, textProcessing, nbest);
        } else {
            task = new TranslationTask(text, textProcessing, nbest);
        }

        try {
            return this.execute(task);
        } catch (Throwable e) {
            if (e instanceof ProcessingException)
                throw new TranslationException("Problem while processing translation", e);
            else if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else
                throw new Error("Unexpected exception: " + e.getMessage(), e);
        }
    }

    private <R extends Serializable> R execute(DistributedCallable<R> task) throws Throwable {
        try {
            return super.submit(task).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            logger.warn(task.getClass().getSimpleName() + " execution failed with exception: " + cause.getMessage(), cause);

            throw cause;
        } catch (InterruptedException | CancellationException e) {
            throw new SystemShutdownException(e);
        }
    }

}
