package eu.modernmt.processing.tags;

/**
 * Created by davide on 17/02/16.
 */
public class _Sentence {

    protected _Token[] tokens;
    protected _Tag[] tags;

    public _Sentence(_Token[] tokens) {
        this(tokens, null);
    }

    public _Sentence(_Token[] tokens, _Tag[] tags) {
        this.tokens = tokens;
        this.tags = tags;
    }

    public _Token[] getTokens() {
        return tokens;
    }

    public _Tag[] getTags() {
        return tags;
    }

    public void setTokens(_Token[] tokens) {
        this.tokens = tokens;
    }

    /**
     * Sets tags of the sentence
     *
     * @param tags is an array of tags <b>ordered by position field</b>.
     */
    public void setTags(_Tag[] tags) {
        this.tags = tags;
    }

    public String getStrippedString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < tokens.length; i++) {
            _Token token = tokens[i];

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
        _Token[] sentence = new _Token[tokens.length + tags.length];
        int sIndex = 0;

        int tagIndex = 0;
        _Tag tag = tags[tagIndex];

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
            _Token token = sentence[i];
            builder.append(token.getText());

            _Tag next = null;
            if (i < sentence.length - 1 && (sentence[i + 1] instanceof _Tag))
                next = (_Tag) sentence[i + 1];

            if (next != null) {
                if (next.hasLeftSpace())
                    builder.append(' ');
            } else if (token.hasRightSpace()) {
                builder.append(' ');
            }
        }

        return builder.toString();
    }

    public static void main(String[] args) throws Throwable {
        _Sentence sentence1 = new _Sentence(new _Token[] {
                new _Token("Ciao", true),
                new _Token("Davide", false),
        }, new _Tag[] {
                new _Tag("<b>", true, false, 1),
                new _Tag("</b>", false, false, 2),
        });

        _Sentence sentence2 = new _Sentence(new _Token[] {
                new _Token("Ciao", true),
                new _Token("Davide", false),
        }, new _Tag[] {
                new _Tag("<b>", false, false, 0),
                new _Tag("</b>", false, false, 2),
        });

        _Sentence sentence3 = new _Sentence(new _Token[] {
                new _Token("Ciao", true),
                new _Token("Davide", false),
        }, new _Tag[] {
                new _Tag("<b>", true, false, 1),
                new _Tag("</b>", false, false, 1),
        });

        _Sentence sentence4 = new _Sentence(new _Token[] {
                new _Token("Ciao", true),
                new _Token("Davide", false),
        }, new _Tag[] {
                new _Tag("<b>", true, false, 2),
                new _Tag("</b>", false, false, 2),
        });

        _Sentence sentence5 = new _Sentence(new _Token[] {
                new _Token("Ciao", true),
                new _Token("Davide", false),
        }, new _Tag[] {
                new _Tag("<b>", true, false, 2),
                new _Tag("</b>", false, false, 2),
        });

        System.out.println(sentence5);
        System.out.println(sentence5.getStrippedString());
    }

}
