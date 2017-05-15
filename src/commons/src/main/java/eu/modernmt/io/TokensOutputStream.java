package eu.modernmt.io;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;

import java.io.*;
import java.util.Iterator;

/**
 * Created by davide on 12/05/17.
 */
public class TokensOutputStream implements Closeable {

    public static String[] toTokensArray(Sentence sentence, boolean printTags, boolean printPlaceholders) {
        int size = printTags ? sentence.length() : sentence.getWords().length;
        String[] tokens = new String[size];

        Iterator<Token> iterator = printTags ? sentence.iterator() : new ArrayIterator<>(sentence.getWords());

        int i = 0;
        while (iterator.hasNext()) {
            Token token = iterator.next();
            String text = printPlaceholders ? token.getPlaceholder() : token.toString();

            tokens[i++] = text.replace(' ', '\u00A0');
        }

        return tokens;
    }

    public static String toString(Sentence sentence, boolean printTags, boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        Iterator<Token> iterator = printTags ? sentence.iterator() : new ArrayIterator<>(sentence.getWords());
        while (iterator.hasNext()) {
            Token token = iterator.next();
            String text = printPlaceholders ? token.getPlaceholder() : token.toString();

            builder.append(text.replace(' ', '\u00A0'));

            if (iterator.hasNext())
                builder.append(' ');
        }

        return builder.toString();
    }

    private final UnixLineWriter writer;
    private final boolean printTags;
    private final boolean printPlaceholders;

    public TokensOutputStream(File file, boolean append, boolean printTags, boolean printPlaceholders) throws FileNotFoundException {
        this.writer = new UnixLineWriter(new FileOutputStream(file, append), DefaultCharset.get());
        this.printTags = printTags;
        this.printPlaceholders = printPlaceholders;
    }

    public TokensOutputStream(OutputStream stream, boolean printTags, boolean printPlaceholders) {
        this.writer = new UnixLineWriter(stream, DefaultCharset.get());
        this.printTags = printTags;
        this.printPlaceholders = printPlaceholders;
    }

    public TokensOutputStream(Writer writer, boolean includeTags, boolean usePlaceholders) {
        this.writer = new UnixLineWriter(writer);
        this.printTags = includeTags;
        this.printPlaceholders = usePlaceholders;
    }

    public void write(Sentence sentence) throws IOException {
        this.writer.writeLine(toString(sentence, printTags, printPlaceholders));
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
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
