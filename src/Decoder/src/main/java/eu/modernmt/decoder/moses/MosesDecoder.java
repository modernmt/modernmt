package eu.modernmt.decoder.moses;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.model.Sentence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 26/11/15.
 */
public class MosesDecoder implements Decoder {

    private static final Logger logger = LogManager.getLogger(MosesDecoder.class);

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
    private HashMap<Long, MosesSession> sessions;

    public MosesDecoder(File inifile) throws IOException {
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
    public DecoderTranslation translate(Sentence text) {
        return translate(text.getStrippedString(), null, 0L, 0).getTranslation(text);
    }

    @Override
    public DecoderTranslation translate(Sentence text, List<ContextDocument> translationContext) {
        return translate(text.getStrippedString(), parse(translationContext), 0L, 0).getTranslation(text);
    }

    @Override
    public DecoderTranslation translate(Sentence text, TranslationSession session) {
        return translate(text.getStrippedString(), null, ((MosesSession) session).getInternalId(), 0).getTranslation(text);
    }

    @Override
    public DecoderTranslation translate(Sentence text, int nbestListSize) {
        return translate(text.getStrippedString(), null, 0L, nbestListSize).getTranslation(text);
    }

    @Override
    public DecoderTranslation translate(Sentence text, List<ContextDocument> translationContext, int nbestListSize) {
        return translate(text.getStrippedString(), parse(translationContext), 0L, nbestListSize).getTranslation(text);
    }

    @Override
    public DecoderTranslation translate(Sentence text, TranslationSession session, int nbestListSize) {
        return translate(text.getStrippedString(), null, ((MosesSession) session).getInternalId(), nbestListSize).getTranslation(text);
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
