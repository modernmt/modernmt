package eu.modernmt.processing;

import eu.modernmt.processing.framework.TextProcessor;

/**
 * Created by davide on 26/01/16.
 */
public class UppercaseProcessor implements TextProcessor<String, String> {

    @Override
    public String call(String param) {
        return param.toUpperCase();
    }

    @Override
    public void close() {

    }

}
