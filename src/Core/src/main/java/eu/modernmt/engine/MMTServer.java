package eu.modernmt.engine;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Sentence;
import eu.modernmt.decoder.Translation;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.decoder.moses.MosesFeature;
import eu.modernmt.engine.tasks.GetFeatureWeightsTask;
import eu.modernmt.engine.tasks.NBestListTask;
import eu.modernmt.engine.tasks.TranslationTask;
import eu.modernmt.network.cluster.Cluster;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingServer;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 09/12/15.
 */
public class MMTServer extends Cluster {

    public static final int[] DEFAULT_SERVER_PORTS = {5000, 5001};

    public static final byte SIGNAL_RESET = 0x01;
    public static final byte REQUEST_FWEIGHTS = 0x03;

    private TranslationEngine engine;
    private HashMap<Long, TranslationSession> sessions;

    public MMTServer(TranslationEngine engine) throws IOException {
        this(engine, DEFAULT_SERVER_PORTS);
    }

    public MMTServer(TranslationEngine engine, int[] ports) throws IOException {
        super(new ZMQMessagingServer(ports[0], ports[1]));
        this.engine = engine;
        this.sessions = new HashMap<>();
    }

    @Override
    public void start() throws IOException {
        super.start();
        sendBroadcastSignal(SIGNAL_RESET, null);
    }

    @Override
    protected byte[] onCustomRequestReceived(byte signal, byte[] payload, int offset, int length) {
        switch (signal) {
            case REQUEST_FWEIGHTS:
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                stream.write(signal);

                Serializable weights = (Serializable) engine.getDecoderWeights();
                if (weights != null)
                    SerializationUtils.serialize(weights, stream);
                return stream.toByteArray();
            default:
                logger.warn("Unknown request received: " + Integer.toHexString(signal));
                return new byte[0];
        }
    }

    // =============================
    //  Decoder Weights
    // =============================

    public Map<MosesFeature, float[]> getFeatureWeights() throws IOException {
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

    public List<ContextDocument> getContext(Reader context, int limit) throws IOException {
        ContextAnalyzer analyzer = engine.getContextAnalyzer();
        return analyzer.getContext(context, engine.getSourceLanguage(), limit);
    }

    public List<ContextDocument> getContext(InputStream context, int limit) throws IOException {
        ContextAnalyzer analyzer = engine.getContextAnalyzer();
        return analyzer.getContext(new InputStreamReader(context), engine.getSourceLanguage(), limit);
    }

    public List<ContextDocument> getContext(String context, int limit) throws IOException {
        ContextAnalyzer analyzer = engine.getContextAnalyzer();
        return analyzer.getContext(context, engine.getSourceLanguage(), limit);
    }

    // =============================
    //  Translation session
    // =============================

    public TranslationSession createTranslationSession(List<ContextDocument> context) throws IOException {
        DistributedTranslationSession session = new DistributedTranslationSession(context);
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

    public String translate(String sentence, boolean textProcessing) throws IOException {
        return execute(new TranslationTask(sentence, textProcessing));
    }

    public String translate(String sentence, long sessionId, boolean textProcessing) throws IOException {
        TranslationSession session = sessions.get(sessionId);
        if (session == null)
            throw new IllegalArgumentException("Invalid session id " + sessionId);

        return execute(new TranslationTask(sentence, session, textProcessing));
    }

    public String translate(String sentence, List<ContextDocument> translationContext, boolean textProcessing) throws IOException {
        return execute(new TranslationTask(sentence, translationContext, textProcessing));
    }

    // =============================
    //  Compute NBest list
    // =============================

    public List<TranslationHypothesis> translate(Sentence sentence, int nbest) throws IOException {
        return execute(new NBestListTask(sentence, nbest));
    }

    public List<TranslationHypothesis> translate(Sentence sentence, long sessionId, int nbest) throws IOException {
        TranslationSession session = sessions.get(sessionId);
        if (session == null)
            throw new IllegalArgumentException("Invalid session id " + sessionId);

        return execute(new NBestListTask(sentence, session, nbest));
    }

    public List<TranslationHypothesis> translate(Sentence sentence, List<ContextDocument> translationContext, int nbest) throws IOException {
        return execute(new NBestListTask(sentence, translationContext, nbest));
    }

    private <R extends Serializable> R execute(DistributedCallable<R> task) throws IOException {
        try {
            return this.submit(task).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException)
                throw (IOException) cause;
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new RuntimeException("Unexpected exception", cause);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
