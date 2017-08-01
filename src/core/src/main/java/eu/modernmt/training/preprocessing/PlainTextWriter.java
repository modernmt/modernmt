package eu.modernmt.training.preprocessing;

import eu.modernmt.io.LineWriter;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Sentence;

import java.io.IOException;

/**
 * Created by davide on 22/08/16.
 */
public class PlainTextWriter extends CorpusWriter {

    @Override
    protected void doWrite(Sentence[] batch, LineWriter writer) throws IOException {
        for (Sentence sentence : batch) {
            String text = TokensOutputStream.toString(sentence, false, true);
            writer.writeLine(text);
        }
    }

}
