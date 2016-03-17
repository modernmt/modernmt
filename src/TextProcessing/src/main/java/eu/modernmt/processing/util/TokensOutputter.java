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

    public static String toString(Sentence sentence, boolean printTags, boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        Iterator<Token> iterator = printTags ? sentence.iterator() : new ArrayIterator<>(sentence.getWords());

        while (iterator.hasNext()) {
            Token token = iterator.next();

            if (printPlaceholders && (token instanceof PlaceholderToken))
                builder.append(((PlaceholderToken) token).getPlaceholder());
            else
                builder.append(token.getText());

            if (iterator.hasNext())
                builder.append(' ');
        }

        return builder.toString();
    }

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
        writer.write(toString(sentence, printTags, printPlaceholders));
        writer.write('\n');
        writer.flush();
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