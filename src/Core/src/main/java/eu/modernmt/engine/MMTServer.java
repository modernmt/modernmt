package eu.modernmt.engine;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Sentence;
import eu.modernmt.decoder.Translation;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.engine.tasks.NBestListTask;
import eu.modernmt.engine.tasks.TranslationTask;
import eu.modernmt.network.cluster.Cluster;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.network.messaging.zeromq.ZMQMessagingServer;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 09/12/15.
 */
public class MMTServer {

    public static final int DEFAULT_SERVER_PORT = 5005;

    private Cluster cluster;
    private TranslationEngine engine;
    private HashMap<Long, TranslationSession> sessions;

    public MMTServer(TranslationEngine engine) throws IOException {
        this(engine, DEFAULT_SERVER_PORT);
    }

    public MMTServer(TranslationEngine engine, int port) throws IOException {
        this.cluster = new Cluster(new ZMQMessagingServer(port));
        this.engine = engine;
        this.sessions = new HashMap<>();
    }

    public boolean isShutdown() {
        return cluster.isShutdown();
    }

    public boolean isTerminated() {
        return cluster.isTerminated();
    }

    public void shutdown() {
        cluster.shutdown();
    }

    public void shutdownNow() {
        cluster.shutdownNow();
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        cluster.awaitTermination(timeout, unit);
    }

    // =============================
    //  Context Analysis
    // =============================

    public List<ContextDocument> getContext(Reader context, int limit) throws IOException {
        ContextAnalyzer analyzer = engine.getContextAnalyzer();
        return analyzer.getContext(context, engine.getSourceLanguage().toLanguageTag(), limit);
    }

    public List<ContextDocument> getContext(InputStream context, int limit) throws IOException {
        ContextAnalyzer analyzer = engine.getContextAnalyzer();
        return analyzer.getContext(new InputStreamReader(context), engine.getSourceLanguage().toLanguageTag(), limit);
    }

    public List<ContextDocument> getContext(String context, int limit) throws IOException {
        ContextAnalyzer analyzer = engine.getContextAnalyzer();
        return analyzer.getContext(context, engine.getSourceLanguage().toLanguageTag(), limit);
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

    public Translation translate(Sentence sentence) throws IOException {
        return submitTranslation(new TranslationTask(sentence));
    }

    public Translation translate(Sentence sentence, long sessionId) throws IOException {
        TranslationSession session = sessions.get(sessionId);
        if (session == null)
            throw new IllegalArgumentException("Invalid session id " + sessionId);

        return submitTranslation(new TranslationTask(sentence, session));
    }

    public Translation translate(String engine, Sentence sentence, List<ContextDocument> translationContext) throws IOException {
        return submitTranslation(new TranslationTask(sentence, translationContext));
    }

    // =============================
    //  Compute NBest list
    // =============================

    public List<TranslationHypothesis> translate(Sentence sentence, int nbest) throws IOException {
        return submitTranslation(new NBestListTask(sentence, nbest));
    }

    public List<TranslationHypothesis> translate(Sentence sentence, long sessionId, int nbest) throws IOException {
        TranslationSession session = sessions.get(sessionId);
        if (session == null)
            throw new IllegalArgumentException("Invalid session id " + sessionId);

        return submitTranslation(new NBestListTask(sentence, session, nbest));
    }

    public List<TranslationHypothesis> translate(Sentence sentence, List<ContextDocument> translationContext, int nbest) throws IOException {
        return submitTranslation(new NBestListTask(sentence, translationContext, nbest));
    }

    private <R extends Serializable> R submitTranslation(DistributedCallable<R> task) throws IOException {
        try {
            return cluster.submit(task).get();
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
