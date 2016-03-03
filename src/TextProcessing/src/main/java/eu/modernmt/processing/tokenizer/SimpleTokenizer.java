package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.AnnotatedString;
import eu.modernmt.processing.framework.ProcessingException;

import java.io.IOException;

/**
 * Created by davide on 19/02/16.
 */
public class SimpleTokenizer implements Tokenizer {

    @Override
    public AnnotatedString call(String param) throws ProcessingException {
        return new AnnotatedString(param, TokenizerOutputTransformer.transform(param, param.split(" ")));
    }

    @Override
    public void close() throws IOException {

    }

}
