package eu.modernmt.decoder.moses;

import eu.modernmt.model.ContextVector;

/**
 * Created by davide on 27/04/16.
 */
class ContextXObject {

    public final int[] keys;
    public final float[] values;

    public static ContextXObject build(ContextVector vector) {
        if (vector != null) {
            int[] keys = new int[vector.size()];
            float[] values = new float[vector.size()];

            int i = 0;
            for (ContextVector.Entry e : vector) {
                keys[i] = e.domain.getId();
                values[i] = e.score;
                i++;
            }

            return new ContextXObject(keys, values);
        } else {
            return null;
        }
    }

    private ContextXObject(int[] keys, float[] values) {
        this.keys = keys;
        this.values = values;
    }

}
