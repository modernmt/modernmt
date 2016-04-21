package eu.modernmt.core.config;

import java.util.Map;

/**
 * Created by davide on 19/04/16.
 */
public class DecoderConfig {

    private Map<String, float[]> weights;

    public Map<String, float[]> getWeights() {
        return weights;
    }

    public void setWeights(Map<String, float[]> weights) {
        this.weights = weights;
    }

}
