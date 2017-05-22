package eu.modernmt.decoder;

import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.util.Map;

/**
 * Created by davide on 26/11/15.
 */
public interface Decoder extends Closeable {

    // Features

    DecoderFeature[] getFeatures();

    float[] getFeatureWeights(DecoderFeature feature);

    void setDefaultFeatureWeights(Map<DecoderFeature, float[]> weights);

    // Translate

    DecoderTranslation translate(Sentence text) throws DecoderException;

    DecoderTranslation translate(Sentence text, ContextVector contextVector) throws DecoderException;

    DecoderTranslation translate(Sentence text, int nbestListSize) throws DecoderException;

    DecoderTranslation translate(Sentence text, ContextVector contextVector, int nbestListSize) throws DecoderException;
}
