package eu.modernmt.training.preprocessing;

import eu.modernmt.io.LineWriter;
import eu.modernmt.io.TokensOutputStream;

import java.io.IOException;

/**
 * Created by davide on 22/08/16.
 */
public class PlainTextWriter extends CorpusWriter {

    @Override
    protected void doWrite(String[][] batch, LineWriter writer) throws IOException {
        StringBuilder builder = new StringBuilder();

        for (String[] tokens : batch) {
            for (int i = 0; i < tokens.length; i++) {
                String token = TokensOutputStream.escapeWhitespaces(tokens[i]);

                if (i > 0)
                    builder.append(' ');
                builder.append(token);
            }

            writer.writeLine(builder.toString());
            builder.setLength(0);
        }
    }

}
