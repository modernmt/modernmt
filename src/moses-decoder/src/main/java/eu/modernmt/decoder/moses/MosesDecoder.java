package eu.modernmt.decoder.moses;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.io.Paths;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.vocabulary.Vocabulary;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 26/11/15.
 */
public class MosesDecoder implements Decoder, DataListener {

    private static final Logger logger = LogManager.getLogger(MosesDecoder.class);

    static {
        try {
            System.loadLibrary("mmt-decoder");
            logger.info("Library mmt-decoder loaded successfully");
        } catch (Throwable e) {
            logger.error("Unable to load library mmt-decoder", e);
            throw e;
        }
    }

    private final FeatureWeightsStorage storage;
    private final HashMap<Long, Long> sessions = new HashMap<>();
    private long nativeHandle;

    public MosesDecoder(File path, Aligner aligner, Vocabulary vocabulary, int threads) throws IOException {
        this.storage = new FeatureWeightsStorage(Paths.join(path, "weights.dat"));

        File iniTemplate = Paths.join(path, "moses.ini");
        MosesINI mosesINI = MosesINI.load(iniTemplate, path);

        Map<String, float[]> featureWeights = storage.getWeights();
        if (featureWeights != null)
            mosesINI.setWeights(featureWeights);

        mosesINI.setThreads(threads);

        File iniFile = File.createTempFile("mmtmoses", "ini");
        iniFile.deleteOnExit();

        FileUtils.write(iniFile, mosesINI.toString(), false);

        this.nativeHandle = instantiate(iniFile.getAbsolutePath(),
                aligner.getNativeHandle(), vocabulary.getNativeHandle());
    }

    private native long instantiate(String inifile, long aligner, long vocabulary);

    // Features

    @Override
    public native MosesFeature[] getFeatures();

    @Override
    public float[] getFeatureWeights(DecoderFeature feature) {
        return getFeatureWeightsFromPointer(((MosesFeature) feature).getNativePointer());
    }

    private native float[] getFeatureWeightsFromPointer(long ptr);

    @Override
    public void setDefaultFeatureWeights(Map<DecoderFeature, float[]> _map) {
        HashMap<String, float[]> map = new HashMap<>(_map.size());

        String[] features = new String[_map.size()];
        float[][] weights = new float[_map.size()][];

        int i = 0;
        for (Map.Entry<DecoderFeature, float[]> entry : _map.entrySet()) {
            features[i] = entry.getKey().getName();
            weights[i] = entry.getValue();

            map.put(features[i], weights[i]);

            i++;
        }

        this.setFeatureWeights(features, weights);

        try {
            this.storage.setWeights(map);
        } catch (IOException e) {
            throw new RuntimeException("Unable to store feature weights", e);
        }
    }

    private native void setFeatureWeights(String[] features, float[][] weights);

    // Translation session

    private long getOrComputeSession(final TranslationSession session) {
        return sessions.computeIfAbsent(session.getId(), key -> {
            ContextXObject context = ContextXObject.build(session.getContextVector());
            return createSession(context.keys, context.values);
        });
    }

    private native long createSession(int[] contextKeys, float[] contextValues);

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
    public DecoderTranslation translate(Sentence text, ContextVector contextVector) {
        return translate(text, contextVector, null, 0);
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
    public DecoderTranslation translate(Sentence text, ContextVector contextVector, int nbestListSize) {
        return translate(text, contextVector, null, nbestListSize);
    }

    @Override
    public DecoderTranslation translate(Sentence text, TranslationSession session, int nbestListSize) {
        return translate(text, null, session, nbestListSize);
    }

    private DecoderTranslation translate(Sentence sentence, ContextVector contextVector, TranslationSession session, int nbest) {
        Word[] sourceWords = sentence.getWords();
        if (sourceWords.length == 0)
            return new DecoderTranslation(new Word[0], sentence, null);

        String text = XUtils.join(sourceWords);

        long sessionId = session == null ? 0L : getOrComputeSession(session);
        ContextXObject context = ContextXObject.build(contextVector);

        if (logger.isDebugEnabled()) {
            logger.debug("Translating: \"" + text + "\"");
        }

        long start = System.currentTimeMillis();
        TranslationXObject xtranslation = this.translate(text, context == null ? null : context.keys, context == null ? null : context.values, sessionId, nbest);
        long elapsed = System.currentTimeMillis() - start;

        DecoderTranslation translation = xtranslation.getTranslation(sentence);
        translation.setElapsedTime(elapsed);

        logger.info("Translation of " + sentence.length() + " words took " + (((double) elapsed) / 1000.) + "s");

        return translation;
    }

    private native TranslationXObject translate(String text, int[] contextKeys, float[] contextValues, long session, int nbest);

    // Updates

    @Override
    public void onDataReceived(TranslationUnit unit) throws Exception {
        int[] sourceSentence = XUtils.encode(unit.sourceSentence.getWords());
        int[] targetSentence = XUtils.encode(unit.targetSentence.getWords());
        int[] alignment = XUtils.encode(unit.alignment);

        updateReceived(unit.channel, unit.channelPosition, unit.domain, sourceSentence, targetSentence, alignment);
    }

    private native void updateReceived(int streamId, long sentenceId, int domainId, int[] sourceSentence, int[] targetSentence, int[] alignment);

    @Override
    public void onDelete(Deletion deletion) throws Exception {
        // TODO: stub implementation
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        long[] ids = getLatestUpdatesIdentifier();

        HashMap<Short, Long> map = new HashMap<>(ids.length);
        for (short i = 0; i < ids.length; i++) {
            if (ids[i] >= 0)
                map.put(i, ids[i]);
        }

        return map;
    }

    private native long[] getLatestUpdatesIdentifier();

    // Shutdown

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeHandle = dispose(nativeHandle);
    }

    @Override
    public void close() {
        sessions.values().forEach(this::destroySession);
        nativeHandle = dispose(nativeHandle);
    }

    private native long dispose(long handle);

}
