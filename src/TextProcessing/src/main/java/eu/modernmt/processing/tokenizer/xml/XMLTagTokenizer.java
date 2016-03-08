package eu.modernmt.processing.tokenizer.xml;

import eu.modernmt.model.Tag;
import eu.modernmt.processing.tokenizer.TokenizedString;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.tokenizer.Tokenizer;

import java.io.IOException;
import java.util.regex.Matcher;

/**
 * Created by davide on 19/02/16.
 */
public class XMLTagTokenizer implements Tokenizer {

    @Override
    public TokenizedString call(TokenizedString text) throws ProcessingException {
        Matcher m = Tag.TagRegex.matcher(text.string);

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            text.setTag(start, end);
        }

        return text;
    }

    @Override
    public void close() throws IOException {
    }

}
