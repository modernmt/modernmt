package eu.modernmt.io;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Word;
import eu.modernmt.xml.XMLUtils;

import java.io.*;
import java.util.Iterator;

/**
 * Created by davide on 12/05/17.
 */
public class TokensOutputStream implements Closeable {

    public static String[] tokens(Sentence sentence, boolean printTags, boolean printPlaceholders) {
        int size = printTags ? sentence.length() : sentence.getWords().length;
        String[] tokens = new String[size];

        Iterator<Token> iterator = printTags ? sentence.iterator() : new ArrayIterator<>(sentence.getWords());

        int i = 0;
        while (iterator.hasNext()) {
            Token token = iterator.next();

            String text;
            if (token instanceof Tag) {
                text = token.getText();
            } else {
                text = printPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                if (printTags)
                    text = XMLUtils.escapeText(text);
            }

            tokens[i++] = text.replace(' ', '\u00A0');
        }

        return tokens;
    }

    public static String serialize(Sentence sentence, boolean printTags, boolean printPlaceholders) {
        String[] parts = tokens(sentence, printTags, printPlaceholders);
        if (parts.length == 0)
            return "";

        int length = 0;
        for (String part : parts)
            length += part.length();

        StringBuilder builder = new StringBuilder(length + parts.length - 1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0)
                builder.append(' ');
            builder.append(parts[i]);
        }

        return builder.toString();
    }

    public static String[] deserialize(String text) {
        if (text.isEmpty())
            return new String[0];

        String[] pieces = text.split(" +");

        for (int i = 0; i < pieces.length; i++)
            pieces[i] = pieces[i].replace('\u00A0', ' ');

        return pieces;
    }

    public static Word[] deserializeWords(String text) {
        if (text.isEmpty())
            return new Word[0];

        return deserializeWords(deserialize(text));
    }

    public static Word[] deserializeWords(String[] tokens) {
        if (tokens.length == 0)
            return new Word[0];

        Word[] words = new Word[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            String leftSpace = i > 0 ? " " : null;
            String rightSpace = i < tokens.length - 1 ? " " : null;
            words[i] = new Word(tokens[i], leftSpace, rightSpace);
        }

        return words;
    }

    private final LineWriter writer;
    private final boolean printTags;
    private final boolean printPlaceholders;

    public TokensOutputStream(File file, boolean append, boolean printTags, boolean printPlaceholders) throws FileNotFoundException {
        this(new UnixLineWriter(new FileOutputStream(file, append), UTF8Charset.get()), printTags, printPlaceholders);
    }

    public TokensOutputStream(OutputStream stream, boolean printTags, boolean printPlaceholders) {
        this(new UnixLineWriter(stream, UTF8Charset.get()), printTags, printPlaceholders);
    }

    public TokensOutputStream(Writer writer, boolean includeTags, boolean usePlaceholders) {
        this(new UnixLineWriter(writer), includeTags, usePlaceholders);
    }

    public TokensOutputStream(LineWriter writer, boolean includeTags, boolean usePlaceholders) {
        this.writer = writer;
        this.printTags = includeTags;
        this.printPlaceholders = usePlaceholders;
    }

    public void write(Sentence sentence) throws IOException {
        this.writer.writeLine(serialize(sentence, printTags, printPlaceholders));
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