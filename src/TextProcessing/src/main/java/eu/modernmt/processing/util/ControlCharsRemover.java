package eu.modernmt.processing.util;

import eu.modernmt.processing.framework.TextProcessor;

import java.util.Map;

/**
 * Created by davide on 12/05/16.
 */
public class ControlCharsRemover implements TextProcessor<String, String> {

    @Override
    public String call(String param, Map<String, Object> metadata) {
        char[] buffer = param.toCharArray();

        int newSize = 0;

        for (int i = 0; i < buffer.length; i++) {
            char c = buffer[i];

            if (c > 0x0008) {
                buffer[newSize] = c;
                newSize++;
            }
        }

        return new String(buffer, 0, newSize);
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
