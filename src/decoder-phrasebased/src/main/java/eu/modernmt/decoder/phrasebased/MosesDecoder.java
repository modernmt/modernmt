package eu.modernmt.decoder.phrasebased;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.DataListener;
import eu.modernmt.decoder.*;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.io.Paths;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 26/11/15.
 */
public class MosesDecoder implements Decoder, DecoderWithFeatures, DecoderWithNBest, DataListener {

    private static final Logger logger = LogManager.getLogger(MosesDecoder.class);

    static {
        try {
            System.loadLibrary("mmt_pbdecoder");
        } catch (Throwable e) {
            logger.error("Unable to load library 'mmt_pbdecoder'", e);
            throw e;
        }

        NativeLogger.initialize();
    }

    private final LanguagePair direction;
    private final FeatureWeightsStorage storage;
    private final HashMap<String, DecoderFeature> featuresMap;
    private final MosesFeature[] features;
    private long nativeHandle;

    public MosesDecoder(File path, int threads) throws IOException {
        String raw = FileUtils.readFileToString(Paths.join(path, "language.info"), DefaultCharset.get());
        String[] parts = raw.trim().split(" ");

        this.direction = new LanguagePair(Locale.forLanguageTag(parts[0]), Locale.forLanguageTag(parts[1]));
        this.storage = new FeatureWeightsStorage(Paths.join(path, "weights.dat"));

        File vocabulary = Paths.join(path, "vocab.vb");
        File iniTemplate = Paths.join(path, "moses.ini");
        MosesINI mosesINI = MosesINI.load(iniTemplate, path);

        Map<String, float[]> featureWeights = storage.getWeights();
        if (featureWeights != null)
            mosesINI.setWeights(featureWeights);

        mosesINI.setThreads(threads);

        File iniFile = File.createTempFile("mmtmoses", "ini");
        iniFile.deleteOnExit();

        FileUtils.write(iniFile, mosesINI.toString(), false);

        this.nativeHandle = instantiate(iniFile.getAbsolutePath(), vocabulary.getAbsolutePath());
        this.features = features();
        this.featuresMap = new HashMap<>(this.features.length);

        for (MosesFeature feature : features)
            this.featuresMap.put(feature.getName(), feature);
    }

    private native long instantiate(String inifile, String vocabulary);

    // DecoderWithFeatures

    @Override
    public MosesFeature[] getFeatures() {
        return features;
    }

    private native MosesFeature[] features();

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

    // Decoder

    @Override
    public void setListener(DecoderListener listener) {
        listener.onTranslationDirectionsChanged(Collections.singleton(this.direction));
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence text) throws DecoderException {
        return translate(direction, text, null, 0);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence text, ContextVector contextVector) throws DecoderException {
        return translate(direction, text, contextVector, 0);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence text, int nbestListSize) throws DecoderException {
        return translate(direction, text, null, nbestListSize);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence sentence, ContextVector contextVector, int nbestListSize) throws DecoderException {
        if (!this.direction.equals(direction))
            throw new UnsupportedLanguageException(direction);

        if (sentence.getWords().length == 0)
            return new Translation(new Word[0], sentence, null);

        String text = XUtils.encodeSentence(sentence);

        ContextXObject context = ContextXObject.build(contextVector);

        if (logger.isDebugEnabled())
            logger.debug("Translating: \"" + text + "\"");

        long start = System.currentTimeMillis();
        TranslationXObject xtranslation = this.xtranslate(text,
                context == null ? null : context.keys,
                context == null ? null : context.values,
                nbestListSize);

        long elapsed = System.currentTimeMillis() - start;

        Translation translation = xtranslation.getTranslation(sentence, this.featuresMap);
        translation.setElapsedTime(elapsed);

        if (logger.isDebugEnabled())
            logger.debug("Translation of " + sentence.length() + " words took " + (((double) elapsed) / 1000.) + "s");

        return translation;
    }

    private native TranslationXObject xtranslate(String text, long[] contextKeys, float[] contextValues, int nbest);

    // DataListener

    @Override
    public void onDataReceived(DataBatch batch) throws Exception {
        DataBatchXObject xbatch = new DataBatchXObject(batch, this.direction);

        this.dataReceived(
                xbatch.tuChannels, xbatch.tuChannelPositions, xbatch.tuMemories,
                xbatch.tuSources, xbatch.tuTargets, xbatch.tuAlignments,

                xbatch.delChannels, xbatch.delChannelPositions, xbatch.delMemories,

                xbatch.channels, xbatch.channelPositions
        );
    }

    private native void dataReceived(
            // Translation units
            short[] tuChannels, long[] tuChannelPositions, long[] tuMemories,
            String[] tuSources, String[] tuTargets, int[][] tuAlignments,

            // Deletions
            short[] delChannels, long[] delChannelPositions, long[] delMemories,

            // Channel positions
            short[] channels, long[] channelPositions
    );

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        HashMap<Short, Long> result = new HashMap<>();

        long[] encoded = getLatestUpdatesIdentifier();
        for (int i = 0; i < encoded.length; i += 2) {
            short channel = (short) encoded[i];
            long value = encoded[i + 1];

            result.putIfAbsent(channel, value);
        }

        return result;
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
        nativeHandle = dispose(nativeHandle);
    }

    private native long dispose(long handle);

    @Override
    public boolean supportsSentenceSplit() {
        return false;
    }
}
