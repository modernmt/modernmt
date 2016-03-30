package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.string.ProcessedString;

/**
 * Created by davide on 19/02/16.
 */
public class SimpleTokenizer implements Tokenizer {

    @Override
    public ProcessedString call(ProcessedString param) {
        String[] tokens = param.toString().split(" +");
        return TokenizerOutputTransformer.transform(param, tokens);
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
