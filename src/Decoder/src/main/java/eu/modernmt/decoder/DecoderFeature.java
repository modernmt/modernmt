package eu.modernmt.decoder;

/**
 * Created by davide on 30/11/15.
 */
public interface DecoderFeature {

    float UNTUNEABLE = Float.MAX_VALUE;

    String getName();

    float[] getWeights();

}
