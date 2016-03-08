package eu.modernmt.processing;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.tokenizer.jflex.AnnotatedString;

import java.io.IOException;

/**
 * Created by davide on 08/03/16.
 */
public class XMLCharRefNormalizer implements TextProcessor<AnnotatedString, AnnotatedString> {

    @Override
    public AnnotatedString call(AnnotatedString text) throws ProcessingException {
        return text;
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }
}
