package eu.modernmt.decoder.phrasebased;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.io.Paths;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 26/11/15.
 */
public class MosesDecoder implements Decoder, DataListenerProvider {

    private static final Logger logger = LogManager.getLogger(MosesDecoder.class);

    static {
        try {
            System.loadLibrary("mmt_pbdecoder");
        } catch (Throwable e) {
            logger.error("Unable to load library 'mmt_pbdecoder'", e);
            throw e;
        }
    }

    private final FeatureWeightsStorage storage;
    private long nativeHandle;

    private ArrayList<DataListener> dataListeners = null;

    public MosesDecoder(File path, int threads) throws IOException {
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

        this.nativeHandle = instantiate(iniFile.getAbsolutePath());
    }

    private native long instantiate(String inifile);

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

    // Translate

    @Override
    public DecoderTranslation translate(Sentence text) {
        return translate(text, null, 0);
    }

    @Override
    public DecoderTranslation translate(Sentence text, ContextVector contextVector) {
        return translate(text, contextVector, 0);
    }

    @Override
    public DecoderTranslation translate(Sentence text, int nbestListSize) {
        return translate(text, null, nbestListSize);
    }

    @Override
    public DecoderTranslation translate(Sentence sentence, ContextVector contextVector, int nbest) {
        Word[] sourceWords = sentence.getWords();
        if (sourceWords.length == 0)
            return new DecoderTranslation(new Word[0], sentence, null);

        String text = XUtils.join(sourceWords);

        ContextXObject context = ContextXObject.build(contextVector);

        if (logger.isDebugEnabled()) {
            logger.debug("Translating: \"" + text + "\"");
        }

        long start = System.currentTimeMillis();
        TranslationXObject xtranslation = this.translate(text,
                context == null ? null : context.keys,
                context == null ? null : context.values,
                nbest);

        long elapsed = System.currentTimeMillis() - start;

        DecoderTranslation translation = xtranslation.getTranslation(sentence);
        translation.setElapsedTime(elapsed);

        logger.info("Translation of " + sentence.length() + " words took " + (((double) elapsed) / 1000.) + "s");

        return translation;
    }

    private native TranslationXObject translate(String text, int[] contextKeys, float[] contextValues, int nbest);

    // DataListenerProvider

    @Override
    public Collection<DataListener> getDataListeners() {
        if (dataListeners == null) {
            long[] nativeListeners = getNativeDataListeners();

            dataListeners = new ArrayList<>(nativeListeners.length);
            for (long handle : nativeListeners)
                dataListeners.add(new NativeDataListener(handle));
        }

        return dataListeners;
    }

    private native long[] getNativeDataListeners();

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
}
