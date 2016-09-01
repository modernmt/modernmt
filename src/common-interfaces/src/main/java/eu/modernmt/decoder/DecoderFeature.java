package eu.modernmt.decoder;

import java.io.Serializable;

/**
 * Created by davide on 09/05/16.
 */
public interface DecoderFeature extends Serializable {

    float UNTUNEABLE_COMPONENT = Float.MAX_VALUE;

    String getName();

    boolean isTunable();
}
