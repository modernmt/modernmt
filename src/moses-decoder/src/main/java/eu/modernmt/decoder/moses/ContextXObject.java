package eu.modernmt.decoder.moses;

import eu.modernmt.context.ContextScore;

import java.util.List;

/**
 * Created by davide on 27/04/16.
 */
class ContextXObject {

    public final String[] keys;
    public final float[] values;

    public static ContextXObject build(List<ContextScore> context) {
        if (context != null) {
            String[] keys = new String[context.size()];
            float[] values = new float[context.size()];

            int i = 0;
            for (ContextScore document : context) {
                keys[i] = document.getId();
                values[i] = document.getScore();
                i++;
            }

            return new ContextXObject(keys, values);
        } else {
            return null;
        }
    }

    private ContextXObject(String[] keys, float[] values) {
        this.keys = keys;
        this.values = values;
    }
}
