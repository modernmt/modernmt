package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.string.XMLEditableString;

import java.util.Map;

/**
 * Created by davide on 19/02/16.
 */
public class SimpleTokenizer implements Tokenizer {

    @Override
    public XMLEditableString call(XMLEditableString param, Map<String, Object> metadata) throws ProcessingException {
        String[] tokens = param.toString().trim().split(" +");

        if (tokens.length == 1 && tokens[0].isEmpty())
            return TokenizerOutputTransformer.transform(param, new String[0]);
        else
            return TokenizerOutputTransformer.transform(param, tokens);
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
