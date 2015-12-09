package eu.modernmt.decoder.moses;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 26/11/15.
 */
public class MosesDecoder implements Decoder {

    static {
        System.loadLibrary("jnimoses");
    }

    private long nativeHandle;

    private MosesINI mosesINI;
    private HashMap<Long, MosesSession> sessions;

    public MosesDecoder(File file) throws IOException {
        this.mosesINI = MosesINI.load(file);
        this.sessions = new HashMap<>();

        this.init(file.getAbsolutePath());
    }

    private native void init(String inifile);

    public native MosesFeature[] getFeatures();

    public native float[] getFeatureWeights(MosesFeature feature);

    public void updateFeatureWeights(Map<String, float[]> featureName2Weights) {
        mosesINI.updateWeights(featureName2Weights);
        try {
            mosesINI.save();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write moses.ini", e);
        }

        this.dispose();
        this.init(mosesINI.getFile().getAbsolutePath());
    }

    @Override
    public TranslationSession openSession(List<ContextDocument> translationContext) {
        long sessionId = createSession(parse(translationContext));
        MosesSession session = new MosesSession(sessionId, translationContext, this);
        this.sessions.put(sessionId, session);

        return session;
    }

    private native long createSession(Map<String, Float> translationContext);

    public void closeSession(long id) {
        MosesSession session = this.sessions.remove(id);
        if (session != null)
            destroySession(id);
    }

    private native void destroySession(long id);

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
        TranslationXObject translation = translate(text.toString(), null, session.getId(), 0);
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
        return translate(text.toString(), null, session.getId(), nbestListSize).getHypotheses(text);
    }

    private native TranslationXObject translate(String text, Map<String, Float> translationContext, long session, int nbest);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    @Override
    public void close() throws IOException {
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
