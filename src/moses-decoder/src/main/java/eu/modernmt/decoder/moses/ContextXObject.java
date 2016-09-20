package eu.modernmt.decoder.moses;

import eu.modernmt.context.ContextScore;

import java.util.List;

/**
 * Created by davide on 27/04/16.
 */
class ContextXObject {

    public final int[] keys;
    public final float[] values;

    public static ContextXObject build(List<ContextScore> context) {
        if (context != null) {
            int[] keys = new int[context.size()];
            float[] values = new float[context.size()];

            int i = 0;
            for (ContextScore document : context) {
                keys[i] = document.getDomain().getId();
                values[i] = document.getScore();
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
