package eu.modernmt.processing.util;

import eu.modernmt.constants.Const;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.framework.PipelineOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Created by davide on 27/01/16.
 */
public class SentenceOutputter implements PipelineOutputStream<Sentence> {

    private final Writer writer;
    private boolean printTags;
    private boolean printPlaceholders;

    public SentenceOutputter(OutputStream stream, boolean printTags, boolean printPlaceholders) {
        this(new OutputStreamWriter(stream, Const.charset.get()), printTags, printPlaceholders);
    }

    public SentenceOutputter(Writer writer, boolean printTags, boolean printPlaceholders) {
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
        writer.write(printTags ? sentence.toString(printPlaceholders) : sentence.getStrippedString(printPlaceholders));
        writer.write('\n');
        writer.flush();
    }

}