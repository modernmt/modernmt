package eu.modernmt.training.preprocessing;

import eu.modernmt.io.LineWriter;

import java.io.IOException;

/**
 * Created by davide on 22/08/16.
 */
public class PlainTextWriter extends CorpusWriter {

    @Override
    protected void doWrite(String[][] batch, LineWriter writer) throws IOException {
        //TODO: use TokensOutputter
        StringBuilder builder = new StringBuilder();

        for (String[] line : batch) {
            for (int i = 0; i < line.length; i++) {
                if (i > 0)
                    builder.append(' ');
                builder.append(line[i]);
            }
            builder.append('\n');

            writer.writeLine(builder.toString());
            builder.setLength(0);
        }
    }

}
