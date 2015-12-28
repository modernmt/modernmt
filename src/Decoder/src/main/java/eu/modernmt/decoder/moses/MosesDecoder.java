package eu.modernmt.decoder.moses;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 26/11/15.
 */
public class MosesDecoder implements Decoder {

    private static final Logger logger = LoggerFactory.getLogger(MosesDecoder.class);

    static {
        try {
            logger.info("Loading jnimoses library");
            System.loadLibrary("jnimoses");
            logger.info("Library jnimoses loaded successfully");
        } catch (Throwable e) {
            logger.error("Unable to load library jnimoses", e);
            throw e;
        }
    }

    private long nativeHandle;

    private File inifile;
    private HashMap<Long, MosesSession> sessions;

    public MosesDecoder(File inifile) throws IOException {
        this.inifile = inifile;
        this.sessions = new HashMap<>();

        this.init(inifile.getAbsolutePath());
    }

    private native void init(String inifile);

    public native MosesFeature[] getFeatures();

    public native float[] getFeatureWeights(MosesFeature feature);

    @Override
    public TranslationSession openSession(long id, List<ContextDocument> translationContext) {
        long internalId = createSession(parse(translationContext));
        MosesSession session = new MosesSession(id, translationContext, this, internalId);
        this.sessions.put(id, session);

        return session;
    }

    private native long createSession(Map<String, Float> translationContext);

    void closeSession(MosesSession session) {
        session = this.sessions.remove(session.getId());
        if (session != null)
            destroySession(session.getInternalId());
    }

    private native void destroySession(long internalId);

    @Override
    public TranslationSession getSession(long id) {
        return sessions.get(id);
    }

    @Override
    public Translation translate(Sentence text) {
        TranslationXObject translation = translate(text.toString(), null, 0L, 0);
        return new Translation(translation.text, text);
    }

    @Override
    public Translation translate(Sentence text, List<ContextDocument> translationContext) {
        TranslationXObject translation = translate(text.toString(), parse(translationContext), 0L, 0);
        return new Translation(translation.text, text);
    }

    @Override
    public Translation translate(Sentence text, TranslationSession session) {
        TranslationXObject translation = translate(text.toString(), null, ((MosesSession) session).getInternalId(), 0);
        return new Translation(translation.text, text);
    }

    @Override
    public List<TranslationHypothesis> translate(Sentence text, int nbestListSize) {
        return translate(text.toString(), null, 0L, nbestListSize).getHypotheses(text);
    }

    @Override
    public List<TranslationHypothesis> translate(Sentence text, List<ContextDocument> translationContext, int nbestListSize) {
        return translate(text.toString(), parse(translationContext), 0L, nbestListSize).getHypotheses(text);
    }

    @Override
    public List<TranslationHypothesis> translate(Sentence text, TranslationSession session, int nbestListSize) {
        return translate(text.toString(), null, ((MosesSession) session).getInternalId(), nbestListSize).getHypotheses(text);
    }

    private native TranslationXObject translate(String text, Map<String, Float> translationContext, long session, int nbest);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    @Override
    public void close() {
        this.sessions.values().forEach(MosesSession::close);
        dispose();
    }

    protected native void dispose();

    private static Map<String, Float> parse(List<ContextDocument> translationContext) {
        if (translationContext == null)
            return null;

        HashMap<String, Float> map = new HashMap<>();
        for (ContextDocument document : translationContext) {
            map.put(document.getId(), document.getScore());
        }

        return map;
    }

}
