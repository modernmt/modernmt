package eu.modernmt.processing;

import eu.modernmt.processing.framework.TextProcessor;

/**
 * Created by davide on 26/01/16.
 */
public class LowercaseProcessor implements TextProcessor<String, String> {

    @Override
    public String call(String param) {
        return param.toLowerCase();
    }

    @Override
    public void close() {

    }

}
