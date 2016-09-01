package eu.modernmt.processing.util;

import eu.modernmt.io.LineWriter;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.PipelineOutputStream;

import java.io.IOException;

/**
 * Created by davide on 27/01/16.
 */
public class SentenceOutputter implements PipelineOutputStream<Sentence> {

    private final LineWriter writer;
    private boolean printTags;
    private boolean printPlaceholders;

    public SentenceOutputter(LineWriter writer, boolean printTags, boolean printPlaceholders) {
        this.writer = writer;
        this.printTags = printTags;
        this.printPlaceholders = printPlaceholders;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void write(Sentence sentence) throws IOException {
        writer.writeLine(printTags ? sentence.toString(printPlaceholders) : sentence.getStrippedString(printPlaceholders));
    }

}