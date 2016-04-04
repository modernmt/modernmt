package eu.modernmt.model;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Created by davide on 17/02/16.
 */
public class Sentence implements Serializable, Iterable<Token> {

    protected Word[] words;
    protected Tag[] tags;

    public Sentence(Word[] words) {
        this(words, null);
    }

    public Sentence(Word[] words, Tag[] tags) {
        this.words = words == null ? new Word[0] : words;
        this.tags = tags == null ? new Tag[0] : tags;
    }

    public Word[] getWords() {
        return words;
    }

    public int length() {
        return words.length + tags.length;
    }

    public Tag[] getTags() {
        return tags;
    }

    public boolean hasTags() {
        return tags.length > 0;
    }

    public boolean hasWords() {
        return words.length > 0;
    }

    public void setWords(Word[] words) {
        this.words = words;
    }

    /**
     * Sets tags of the sentence
     *
     * @param tags is an array of tags <b>ordered by position field</b>.
     */
    public void setTags(Tag[] tags) {
        this.tags = tags;
    }

    public String getStrippedString(boolean withPlaceholders) {
        StringBuilder builder = new StringBuilder();

        boolean foundFirstWord = false;
        boolean printSpace = false;
        for (Token token : this) {
            if (token instanceof Tag) {
                printSpace = true;
            } else {
                if (printSpace && foundFirstWord)
                    builder.append(' ');

                foundFirstWord = true;

                String text = withPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                builder.append(text);
                printSpace = token.hasRightSpace();
            }
        }

        return builder.toString();
    }

    public String toString(boolean withPlaceholders) {
        StringBuilder builder = new StringBuilder();
        Iterator<Token> iterator = this.iterator();

        while (iterator.hasNext()) {
            Token token = iterator.next();
            append(builder, token, withPlaceholders);

            if (token.hasRightSpace())
                builder.append(token.getRightSpace());
        }

        return builder.toString();
    }

    private static void append(StringBuilder builder, Token token, boolean withPlaceholders) {
        if (token instanceof Tag) {
            builder.append(token.getText());
        } else {
            String text = withPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
            char[] chars = text.toCharArray();

            for (char c : chars) {
                switch (c) {
                    case '"':
                        builder.append("&quot;");
                        break;
                    case '&':
                        builder.append("&amp;");
                        break;
                    case '\'':
                        builder.append("&apos;");
                        break;
                    case '<':
                        builder.append("&lt;");
                        break;
                    case '>':
                        builder.append("&gt;");
                        break;
                    default:
                        builder.append(c);
                        break;
                }
            }
        }
    }

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public Iterator<Token> iterator() {
        return new Iterator<Token>() {

            private final Token[] tokens = Sentence.this.words;
            private final Tag[] tags = Sentence.this.tags;

            private int tokenIndex = 0;
            private int tagIndex = 0;

            @Override
            public boolean hasNext() {
                return tokenIndex < tokens.length || tagIndex < tags.length;
            }

            @Override
            public Token next() {
                Token nextToken = tokenIndex < tokens.length ? tokens[tokenIndex] : null;
                Tag nextTag = tagIndex < tags.length ? tags[tagIndex] : null;

                if (nextTag != null && (nextToken == null || tokenIndex == nextTag.getPosition())) {
                    tagIndex++;
                    return nextTag;
                } else {
                    tokenIndex++;
                    return nextToken;
                }
            }
        };
    }
}
