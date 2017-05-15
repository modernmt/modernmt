package eu.modernmt.io;

import eu.modernmt.model.Sentence;

import java.io.*;

/**
 * Created by davide on 12/05/17.
 */
public class SentenceOutputStream implements Closeable {

    private final UnixLineWriter writer;
    private final boolean printTags;
    private final boolean printPlaceholders;

    public SentenceOutputStream(File file, boolean append, boolean printTags, boolean printPlaceholders) throws FileNotFoundException {
        this.writer = new UnixLineWriter(new FileOutputStream(file, append), DefaultCharset.get());
        this.printTags = printTags;
        this.printPlaceholders = printPlaceholders;
    }

    public SentenceOutputStream(OutputStream stream, boolean printTags, boolean printPlaceholders) {
        this.writer = new UnixLineWriter(stream, DefaultCharset.get());
        this.printTags = printTags;
        this.printPlaceholders = printPlaceholders;
    }

    public SentenceOutputStream(Writer writer, boolean includeTags, boolean usePlaceholders) {
        this.writer = new UnixLineWriter(writer);
        this.printTags = includeTags;
        this.printPlaceholders = usePlaceholders;
    }

    public void write(Sentence sentence) throws IOException {
        writer.writeLine(printTags ? sentence.toString(printPlaceholders) : sentence.getStrippedString(printPlaceholders));
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }

}
