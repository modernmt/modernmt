package eu.modernmt.engine;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by davide on 10/12/15.
 */
public class ServerStatus implements Serializable {

    public Map<String, float[]> decoderWeights = null;

}
