package eu.modernmt.model;

import java.io.Serializable;

/**
 * Created by davide on 17/02/16.
 */
public class Sentence implements Serializable {

    protected Token[] tokens;
    protected Tag[] tags;

    public Sentence(Token[] tokens) {
        this(tokens, null);
    }

    public Sentence(Token[] tokens, Tag[] tags) {
        this.tokens = tokens == null ? new Token[0] : tokens;
        this.tags = tags;
    }

    public Token[] getTokens() {
        return tokens;
    }

    public Tag[] getTags() {
        return tags;
    }

    public boolean hasTags() {
        return tags != null && tags.length > 0;
    }

    public boolean hasTokens() {
        return tokens.length > 0;
    }

    public void setTokens(Token[] tokens) {
        this.tokens = tokens;
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

        for (int i = 0; i < tokens.length; i++) {
            Token token = tokens[i];

            builder.append(toString(token, withPlaceholders));
            if (i < tokens.length - 1 && token.hasRightSpace())
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
     */
    public String toString(boolean withPlaceholders) {
        if (tags == null || tags.length == 0)
            return getStrippedString(withPlaceholders);

        // Merging tags and tokens arrays into a single array: sentence
        Token[] sentence = new Token[tokens.length + tags.length];
        int sIndex = 0;

        int tagIndex = 0;
        Tag tag = tags[tagIndex];


        for (int i = 0; i < tokens.length; i++) {
            while (tag != null && i == tag.getPosition()) {
                sentence[sIndex++] = tag;
                tagIndex++;
                tag = tagIndex < tags.length ? tags[tagIndex] : null;
            }

            sentence[sIndex++] = tokens[i];
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

}
