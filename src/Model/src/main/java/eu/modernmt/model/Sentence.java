package eu.modernmt.model;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Created by davide on 17/02/16.
 */
public class Sentence implements Serializable, Iterable<Token> {

    protected Token[] words;
    protected Tag[] tags;

    public Sentence(Token[] words) {
        this(words, null);
    }

    public Sentence(Token[] words, Tag[] tags) {
        this.words = words == null ? new Token[0] : words;
        this.tags = tags == null ? new Tag[0] : tags;
    }

    public Token[] getWords() {
        return words;
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

    public void setWords(Token[] words) {
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

    /**
     * @deprecated you should access directly the tokens array and print them
     * in your custom way.
     */
    @Deprecated
    public String getStrippedString(boolean withPlaceholders) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            Token token = words[i];

            builder.append(toString(token, withPlaceholders));
            if (i < words.length - 1 && token.hasRightSpace())
                builder.append(' ');
        }

        return builder.toString();
    }

    /**
     * Create a representation of the sentence including tags, this table
     * shows the behaviour of the algorithm respect to spaces information:
     * <p>
     * TTRX = Token has right space
     * TALX = Tag has left space
     * TARX = Tag has right space
     * TATY = Tag type (O = opening, E = empty, C = closing)
     * <p>
     * TTRX TALX TARX TATY     Result              Example
     * 0    x    x    x        Word<tag>Word       That<b>'s
     * 1    0    1    x        Word<tag> Word      Hello<b> World
     * 1    1    0    x        Word <tag>Word      Hello <b>World
     * 1    0    0    O        Word <b>Word        Hello <b>World
     * 1    0    0    E        Word <b/>Word       Hello <b/>World
     * 1    0    0    C        Word</b> Word       Hello</b> World
     * 1    1    1    O        Word <b>Word        Hello <b>World
     * 1    1    1    E        Word <b/>Word       Hello <b/>World
     * 1    1    1    C        Word</b> Word       Hello</b> World
     * <p>
     * If more there are more consecutive tags, this algorithm ensures that
     * only one space it will be printed. The position of the single space is
     * then decided by the first word and the consecutive tags.
     *
     * @return string representation including tags
     * @deprecated this logic should be embedded in a postprocess task while this
     * function should only iterate over tokens and print ' ' if hasRightSpace is true.
     */
    @Deprecated
    public String toString(boolean withPlaceholders) {
        if (tags.length == 0)
            return getStrippedString(withPlaceholders);

        // Merging tags and tokens arrays into a single array: sentence
        Token[] sentence = new Token[words.length + tags.length];
        int sIndex = 0;

        int tagIndex = 0;
        Tag tag = tags[tagIndex];


        for (int i = 0; i < words.length; i++) {
            while (tag != null && i == tag.getPosition()) {
                sentence[sIndex++] = tag;
                tagIndex++;
                tag = tagIndex < tags.length ? tags[tagIndex] : null;
            }

            sentence[sIndex++] = words[i];
        }

        for (int i = tagIndex; i < tags.length; i++) {
            sentence[sIndex++] = tags[i];
        }

        // Outputting
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < sentence.length; i++) {
            Token token = sentence[i];

            builder.append(toString(token, withPlaceholders));

            Tag nextTag = null;
            if (i < sentence.length - 1 && (sentence[i + 1] instanceof Tag))
                nextTag = (Tag) sentence[i + 1];

            if (nextTag != null) {
                boolean isSpacedClosingComment = (nextTag.isComment() && nextTag.isClosingTag() && nextTag.hasLeftSpace());
                boolean mustPrintSpace;

                if (isSpacedClosingComment) {
                    builder.append(' ');
                    mustPrintSpace = false;
                } else if (!token.hasRightSpace()) {
                    mustPrintSpace = false;
                } else if (nextTag.hasLeftSpace() == nextTag.hasRightSpace()) {
                    if (nextTag.isClosingTag()) {
                        mustPrintSpace = true;
                    } else {
                        builder.append(' ');
                        mustPrintSpace = false;
                    }
                } else if (nextTag.hasLeftSpace()) {
                    builder.append(' ');
                    mustPrintSpace = false;
                } else {
                    mustPrintSpace = true;
                }

                while (nextTag != null) {
                    builder.append(nextTag);
                    i++;

                    boolean isSpacedOpeningComment = (nextTag.isComment() && nextTag.isOpeningTag() && nextTag.hasRightSpace());

                    if (isSpacedOpeningComment) {
                        builder.append(' ');
                        mustPrintSpace = false;
                    }

                    if (i < sentence.length - 1 && (sentence[i + 1] instanceof Tag)) {
                        nextTag = (Tag) sentence[i + 1];

                        isSpacedClosingComment = (nextTag.isComment() && nextTag.isClosingTag() && nextTag.hasLeftSpace());

                        if (isSpacedClosingComment || (mustPrintSpace && !nextTag.isClosingTag())) {
                            builder.append(' ');
                            mustPrintSpace = false;
                        }
                    } else {
                        nextTag = null;
                        if (mustPrintSpace)
                            builder.append(' ');
                    }
                }
            } else if (token.hasRightSpace() && i < sentence.length - 1) {
                builder.append(' ');
            }
        }

        return builder.toString().trim();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    private static String toString(Token token, boolean withPlaceholders) {
        if (withPlaceholders && (token instanceof PlaceholderToken))
            return ((PlaceholderToken) token).getPlaceholder();
        else
            return token.getText();
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
