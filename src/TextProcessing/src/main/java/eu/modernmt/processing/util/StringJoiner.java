package eu.modernmt.processing.util;

import eu.modernmt.processing.framework.TextProcessor;

/**
 * Created by davide on 27/01/16.
 */
public class StringJoiner implements TextProcessor<String[], String> {

    @Override
    public String call(String[] pieces) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pieces.length; i++) {
            if (i > 0)
                builder.append(' ');
            builder.append(pieces[i]);
        }

        return builder.toString();
    }

    @Override
    public void close() {
    }

}