package eu.modernmt.decoder.phrasebased;

import eu.modernmt.model.ContextVector;

/**
 * Created by davide on 27/04/16.
 */
class ContextXObject {

    public final long[] keys;
    public final float[] values;

    public static ContextXObject build(ContextVector vector) {
        if (vector != null) {
            long[] keys = new long[vector.size()];
            float[] values = new float[vector.size()];

            int i = 0;
            for (ContextVector.Entry e : vector) {
                keys[i] = e.memory.getId();
                values[i] = e.score;
                i++;
            }

            return new ContextXObject(keys, values);
        } else {
            return null;
        }
    }

    private ContextXObject(long[] keys, float[] values) {
        this.keys = keys;
        this.values = values;
    }

}
