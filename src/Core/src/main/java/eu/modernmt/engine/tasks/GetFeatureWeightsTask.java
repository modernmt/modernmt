package eu.modernmt.engine.tasks;

import eu.modernmt.decoder.moses.MosesDecoder;
import eu.modernmt.decoder.moses.MosesFeature;
import eu.modernmt.engine.MMTWorker;
import eu.modernmt.engine.TranslationEngine;
import eu.modernmt.network.cluster.DistributedCallable;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by davide on 09/12/15.
 */
public class GetFeatureWeightsTask extends DistributedCallable<HashMap<MosesFeature, float[]>> {

    @Override
    public MMTWorker getWorker() {
        return (MMTWorker) super.getWorker();
    }

    @Override
    public HashMap<MosesFeature, float[]> call() throws IOException {
        TranslationEngine engine = getWorker().getEngine();
        MosesDecoder decoder = (MosesDecoder) engine.getDecoder();

        HashMap<MosesFeature, float[]> result = new HashMap<>();
        for (MosesFeature feature : decoder.getFeatures()) {
            float[] weights = feature.isTunable() ? decoder.getFeatureWeights(feature) : null;
            result.put(feature, weights);
        }

        return result;
    }

}
