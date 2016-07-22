package eu.modernmt.processing.util;

import eu.modernmt.io.LineWriter;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;
import eu.modernmt.processing.framework.PipelineOutputStream;

import java.io.IOException;
import java.util.Iterator;

/**
 * Created by davide on 27/01/16.
 */
public class TokensOutputter implements PipelineOutputStream<Sentence> {

    private final LineWriter writer;
    private boolean printTags;
    private boolean printPlaceholders;

    public static String toString(Sentence sentence, boolean printTags, boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        Iterator<Token> iterator = printTags ? sentence.iterator() : new ArrayIterator<>(sentence.getWords());

        while (iterator.hasNext()) {
            Token token = iterator.next();

            if (printPlaceholders)
                builder.append(token.getPlaceholder());
            else
                builder.append(token);

            if (iterator.hasNext())
                builder.append(' ');
        }

        return builder.toString();
    }

    public TokensOutputter(LineWriter writer, boolean printTags, boolean printPlaceholders) {
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
        writer.writeLine(toString(sentence, printTags, printPlaceholders));
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