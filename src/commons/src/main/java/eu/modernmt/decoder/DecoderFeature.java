package eu.modernmt.decoder;

import java.io.Serializable;

/**
 * Created by davide on 23/05/17.
 */
public interface DecoderFeature extends Serializable {

    float UNTUNEABLE_COMPONENT = Float.MAX_VALUE;

    String getName();

    boolean isTunable();

}