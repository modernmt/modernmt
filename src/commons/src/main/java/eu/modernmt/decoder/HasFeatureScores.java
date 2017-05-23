package eu.modernmt.decoder;

import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public interface HasFeatureScores extends Comparable<HasFeatureScores> {

    float getTotalScore();

    Map<DecoderFeature, float[]> getScores();

}
