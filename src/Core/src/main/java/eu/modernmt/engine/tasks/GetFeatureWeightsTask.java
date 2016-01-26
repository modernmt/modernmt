package eu.modernmt.engine.tasks;

import eu.modernmt.decoder.moses.MosesDecoder;
import eu.modernmt.decoder.moses.MosesFeature;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.network.cluster.DistributedCallable;

import java.util.HashMap;

/**
 * Created by davide on 09/12/15.
 */
public class GetFeatureWeightsTask extends DistributedCallable<HashMap<MosesFeature, float[]>> {

    @Override
    public SlaveNode getWorker() {
        return (SlaveNode) super.getWorker();
    }

    @Override
    public HashMap<MosesFeature, float[]> call() {
        MosesDecoder decoder = (MosesDecoder) getWorker().getDecoder();

        HashMap<MosesFeature, float[]> result = new HashMap<>();
        for (MosesFeature feature : decoder.getFeatures()) {
            float[] weights = feature.isTunable() ? decoder.getFeatureWeights(feature) : null;
            result.put(feature, weights);
        }

        return result;
    }

}
