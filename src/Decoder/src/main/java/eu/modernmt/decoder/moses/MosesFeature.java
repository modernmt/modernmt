package eu.modernmt.decoder.moses;

import eu.modernmt.decoder.DecoderFeature;

/**
 * Created by davide on 30/11/15.
 */
public class MosesFeature implements DecoderFeature {

    private String name;
    private float[] weights;

    public MosesFeature(String name, float[] weights) {
        this.name = name;
        this.weights = weights;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float[] getWeights() {
        return weights;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        text.append(name);

        if (weights == null) {
            text.append("[NULL]");
        } else {
            text.append('[');
            for (int i = 0; i < weights.length; i++) {
                if (weights[i] == UNTUNEABLE)
                    text.append('*');
                else
                    text.append(weights[i]);

                if (i < weights.length - 1)
                    text.append(' ');
            }
            text.append(']');
        }

        return text.toString();
    }
}
