package eu.modernmt.processing.util;

import eu.modernmt.processing.framework.TextProcessor;

/**
 * Created by davide on 27/01/16.
 */
public class Splitter implements TextProcessor<String, String[]> {

    @Override
    public String[] call(String param) {
        return param.split("\\s+");
    }

    @Override
    public void close() {
    }
    
}