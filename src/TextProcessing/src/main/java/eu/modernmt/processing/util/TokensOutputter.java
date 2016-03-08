package eu.modernmt.processing.util;

import eu.modernmt.config.Config;
import eu.modernmt.model.PlaceholderToken;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;
import eu.modernmt.processing.framework.PipelineOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

/**
 * Created by davide on 27/01/16.
 */
public class TokensOutputter implements PipelineOutputStream<Sentence> {

    private final Writer writer;
    private boolean printTags;
    private boolean printPlaceholders;

    public TokensOutputter(OutputStream stream, boolean printTags, boolean printPlaceholders) {
        this(new OutputStreamWriter(stream, Config.charset.get()), printTags, printPlaceholders);
    }

    public TokensOutputter(Writer writer, boolean printTags, boolean printPlaceholders) {
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
        Iterator<Token> iterator = printTags ? sentence.iterator() : new ArrayIterator<>(sentence.getWords());

        while (iterator.hasNext()) {
            Token token = iterator.next();
            writer.write(toString(token));

            if (iterator.hasNext())
                writer.write(' ');
        }

        writer.write('\n');
        writer.flush();
    }

    private String toString(Token token) {
        if (printPlaceholders && (token instanceof PlaceholderToken))
            return ((PlaceholderToken) token).getPlaceholder();
        else
            return token.getText();
    }

    protected static final class ArrayIterator<V> implements Iterator<V> {

        private V[] array;
        private int i;

        public ArrayIterator(V[] array) {
            this.array = array;
            i = 0;
        }

        @Override
        public boolean hasNext() {
            return i < array.length;
        }

        @Override
        public V next() {
            return array[i++];
        }
    }

}