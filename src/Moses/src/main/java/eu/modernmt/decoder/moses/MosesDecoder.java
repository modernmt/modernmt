package eu.modernmt.decoder.moses;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private File iniFile;
    private HashMap<Long, Long> sessions;

    public MosesDecoder(File iniFile) throws IOException {
        this.sessions = new HashMap<>();
        this.iniFile = iniFile;
    }

    private native void init(String inifile);


    @Override
    public void load() throws IOException {
        if (!iniFile.isFile())
            throw new IOException("Invalid INI file: " + iniFile);

        this.init(iniFile.getAbsolutePath());
    }

    // Features

    @Override
    public native MosesFeature[] getFeatures();

    @Override
    public float[] getFeatureWeights(DecoderFeature feature) {
        return getFeatureWeightsFromPointer(((MosesFeature) feature).getNativePointer());
    }

    private native float[] getFeatureWeightsFromPointer(long ptr);

    @Override
    public void setDefaultFeatureWeights(Map<DecoderFeature, float[]> map) {
        Set<DecoderFeature> keys = map.keySet();
        String[] features = new String[keys.size()];
        float[][] weights = new float[keys.size()][];

        int i = 0;
        for (DecoderFeature feature : keys) {
            features[i] = feature.getName();
            weights[i] = map.get(feature);

            i++;
        }

        this.setFeatureWeights(features, weights);
    }

    private native void setFeatureWeights(String[] features, float[][] weights);

    // Translation session

    private long getOrComputeSession(final TranslationSession session) {
        return sessions.computeIfAbsent(session.getId(), key -> {
            ContextXObject context = ContextXObject.build(session.getTranslationContext());
            return createSession(context.keys, context.values);
        });
    }

    private native long createSession(String[] contextKeys, float[] contextValues);

    @Override
    public void closeSession(TranslationSession session) {
        Long internalId = this.sessions.remove(session.getId());
        if (internalId != null) {
            this.destroySession(internalId);

            if (logger.isDebugEnabled())
                logger.debug(String.format("Session %d(%d) destroyed.", session.getId(), internalId));
        }
    }

    private native void destroySession(long internalId);

    // Translate

    @Override
    public DecoderTranslation translate(Sentence text) {
        return translate(text, null, null, 0);
    }

    @Override
    public DecoderTranslation translate(Sentence text, List<ContextDocument> translationContext) {
        return translate(text, translationContext, null, 0);
    }

    @Override
    public DecoderTranslation translate(Sentence text, TranslationSession session) {
        return translate(text, null, session, 0);
    }

    @Override
    public DecoderTranslation translate(Sentence text, int nbestListSize) {
        return translate(text, null, null, nbestListSize);
    }

    @Override
    public DecoderTranslation translate(Sentence text, List<ContextDocument> translationContext, int nbestListSize) {
        return translate(text, translationContext, null, nbestListSize);
    }

    @Override
    public DecoderTranslation translate(Sentence text, TranslationSession session, int nbestListSize) {
        return translate(text, null, session, nbestListSize);
    }

    private DecoderTranslation translate(Sentence sentence, List<ContextDocument> translationContext, TranslationSession session, int nbest) {
        String text = serialize(sentence.getWords());
        long sessionId = session == null ? 0L : getOrComputeSession(session);
        ContextXObject context = ContextXObject.build(translationContext);

        if (logger.isDebugEnabled()) {
            logger.debug("Translating: \"" + text + "\"");
        }

        long start = System.currentTimeMillis();
        DecoderTranslation translation = this.translate(text, context == null ? null : context.keys, context == null ? null : context.values, sessionId, nbest).getTranslation(sentence);
        long elapsed = System.currentTimeMillis() - start;
        translation.setElapsedTime(elapsed);

        if (logger.isDebugEnabled()) {
            logger.debug("Best translation: \"" + serialize(translation.getWords()) + "\"");
        }

        logger.info("Translation of " + sentence.length() + " words took " + (((double) elapsed) / 1000.) + "s");

        return translation;
    }

    private static String serialize(Word[] words) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            text.append(words[i].getPlaceholder());

            if (i < words.length - 1)
                text.append(' ');
        }

        return text.toString();
    }

    private native TranslationXObject translate(String text, String[] contextKeys, float[] contextValues, long session, int nbest);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    @Override
    public void close() {
        this.sessions.values().forEach(this::destroySession);
        dispose();
    }

    protected native void dispose();

}
