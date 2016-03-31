package eu.modernmt.model;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Created by davide on 17/02/16.
 */
public class _Sentence implements Serializable, Iterable<_Token> {

    protected _Word[] words;
    protected _Tag[] tags;

    public _Sentence(_Word[] words) {
        this(words, null);
    }

    public _Sentence(_Word[] words, _Tag[] tags) {
        this.words = words == null ? new _Word[0] : words;
        this.tags = tags == null ? new _Tag[0] : tags;
    }

    public _Token[] getWords() {
        return words;
    }

    public int length() {
        return words.length + tags.length;
    }

    public _Tag[] getTags() {
        return tags;
    }

    public boolean hasTags() {
        return tags.length > 0;
    }

    public boolean hasWords() {
        return words.length > 0;
    }

    public void setWords(_Word[] words) {
        this.words = words;
    }

    /**
     * Sets tags of the sentence
     *
     * @param tags is an array of tags <b>ordered by position field</b>.
     */
    public void setTags(_Tag[] tags) {
        this.tags = tags;
    }

    public String getStrippedString(boolean withPlaceholders) {
        StringBuilder builder = new StringBuilder();

        boolean foundFirstWord = false;
        boolean printSpace = false;
        for (_Token token : this) {
            if (token instanceof _Tag) {
                printSpace = true;
            } else {
                if (printSpace && foundFirstWord)
                    builder.append(' ');

                foundFirstWord = true;

                builder.append(withPlaceholders ? token.getPlaceholder() : token.getText());
                printSpace = token.hasRightSpace();
            }
        }

        return builder.toString();
    }

    public String toString(boolean withPlaceholders) {
        StringBuilder builder = new StringBuilder();
        Iterator<_Token> iterator = this.iterator();

        while (iterator.hasNext()) {
            _Token token = iterator.next();
            append(builder, token, withPlaceholders);

            if (token.hasRightSpace())
                builder.append(token.getRightSpace());
        }

        return builder.toString();
    }

    private static void append(StringBuilder builder, _Token token, boolean withPlaceholders) {
        if (token instanceof _Tag) {
            builder.append(token.getText());
        } else {
            char[] chars = (withPlaceholders ? token.getPlaceholder() : token.getText()).toCharArray();

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
    public Iterator<_Token> iterator() {
        return new Iterator<_Token>() {

            private final _Token[] tokens = _Sentence.this.words;
            private final _Tag[] tags = _Sentence.this.tags;

            private int tokenIndex = 0;
            private int tagIndex = 0;

            @Override
            public boolean hasNext() {
                return tokenIndex < tokens.length || tagIndex < tags.length;
            }

            @Override
            public _Token next() {
                _Token nextToken = tokenIndex < tokens.length ? tokens[tokenIndex] : null;
                _Tag nextTag = tagIndex < tags.length ? tags[tagIndex] : null;

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
