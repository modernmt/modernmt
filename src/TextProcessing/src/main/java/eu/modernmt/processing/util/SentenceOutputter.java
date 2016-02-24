package eu.modernmt.processing.util;

import eu.modernmt.config.Config;
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

    public SentenceOutputter(OutputStream stream, boolean printTags) {
        this(new OutputStreamWriter(stream, Config.charset.get()), printTags);
    }

    public SentenceOutputter(Writer writer, boolean printTags) {
        this.writer = writer;
        this.printTags = printTags;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void write(Sentence value) throws IOException {
        writer.write(printTags ? value.toString() : value.getStrippedString());
        writer.write('\n');
    }

}