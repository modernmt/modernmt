package eu.modernmt.core.facade.operations;

import eu.modernmt.decoder.moses.MosesDecoder;
import eu.modernmt.decoder.moses.MosesFeature;

import java.util.HashMap;

/**
 * Created by davide on 09/12/15.
 */
public class GetFeatureWeightsOperation extends Operation<HashMap<MosesFeature, float[]>> {

    @Override
    public HashMap<MosesFeature, float[]> call() {
        MosesDecoder decoder = (MosesDecoder) getEngine().getDecoder();

        HashMap<MosesFeature, float[]> result = new HashMap<>();
        for (MosesFeature feature : decoder.getFeatures()) {
            float[] weights = feature.isTunable() ? decoder.getFeatureWeights(feature) : null;
            result.put(feature, weights);
        }

        return result;
    }

}
