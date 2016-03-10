package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.ProcessingException;

import java.io.IOException;

/**
 * Created by davide on 19/02/16.
 */
public class SimpleTokenizer implements Tokenizer {

    @Override
    public TokenizedString call(TokenizedString param) throws ProcessingException {
        TokenizerOutputTransformer.transform(param, param.string.split(" +"));
        return param;
    }

    @Override
    public void close() throws IOException {

    }

}
