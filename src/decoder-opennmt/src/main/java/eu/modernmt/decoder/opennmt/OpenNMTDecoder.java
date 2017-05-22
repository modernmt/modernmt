package eu.modernmt.decoder.opennmt;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by davide on 22/05/17.
 */
public class OpenNMTDecoder implements Decoder, DataListener {

    public OpenNMTDecoder(File modelPath) {

    }

    // Decoder

    @Override
    public DecoderFeature[] getFeatures() {
        throw new UnsupportedOperationException("Decoder features not supported by Neural Decoder");
    }

    @Override
    public float[] getFeatureWeights(DecoderFeature feature) {
        throw new UnsupportedOperationException("Decoder features not supported by Neural Decoder");
    }

    @Override
    public void setDefaultFeatureWeights(Map<DecoderFeature, float[]> weights) {
        throw new UnsupportedOperationException("Decoder features not supported by Neural Decoder");
    }

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
    public DecoderTranslation translate(Sentence text, ContextVector contextVector, int nbestListSize) {
        if (nbestListSize > 0)
            throw new UnsupportedOperationException("N-Best not supported by current Neural Decoder implementation");


        return null;
    }

    // DataListener

    @Override
    public void onDataReceived(TranslationUnit unit) throws Exception {

    }

    @Override
    public void onDelete(Deletion deletion) throws Exception {

    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return null;
    }

    // Closeable

    @Override
    public void close() throws IOException {

    }
}
