package eu.modernmt.decoder;

import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public interface DecoderWithFeatures {

    DecoderFeature[] getFeatures();

    float[] getFeatureWeights(DecoderFeature feature);

    void setDefaultFeatureWeights(Map<DecoderFeature, float[]> weights);

}
