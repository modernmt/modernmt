package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.string.XMLEditableString;

/**
 * Created by davide on 19/02/16.
 */
public class SimpleTokenizer implements Tokenizer {

    @Override
    public XMLEditableString call(XMLEditableString param) {
        String[] tokens = param.toString().split(" +");
        return TokenizerOutputTransformer.transform(param, tokens);
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
