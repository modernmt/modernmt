package eu.modernmt.model;

/**
 * Created by davide on 17/02/16.
 */
public class Sentence {

    protected Token[] tokens;
    protected Tag[] tags;

    public Sentence(Token[] tokens) {
        this(tokens, null);
    }

    public Sentence(Token[] tokens, Tag[] tags) {
        this.tokens = tokens;
        this.tags = tags;
    }

    public Token[] getTokens() {
        return tokens;
    }

    public Tag[] getTags() {
        return tags;
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

    public String getStrippedString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < tokens.length; i++) {
            Token token = tokens[i];

            builder.append(token.getText());
            if (i < tokens.length - 1 && token.hasRightSpace())
                builder.append(' ');
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        if (tags == null || tags.length == 0)
            return getStrippedString();

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

            builder.append(token);

            Tag next = null;
            if (i < sentence.length - 1 && (sentence[i + 1] instanceof Tag))
                next = (Tag) sentence[i + 1];

            if (next != null) {
                if (next.hasLeftSpace())
                    builder.append(' ');
            } else if (token.hasRightSpace() && i < sentence.length - 1) {
                builder.append(' ');
            }
        }

        return builder.toString();
    }

}
