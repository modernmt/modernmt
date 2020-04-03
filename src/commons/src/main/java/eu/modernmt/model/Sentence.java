package eu.modernmt.model;

import eu.modernmt.xml.XMLUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Created by davide on 17/02/16.
 */
public class Sentence implements Serializable, Iterable<Token> {

    private final Word[] words;
    private Tag[] tags;
    private Map<String, Annotation> annotations;

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

    /**
     * Sets tags of the sentence
     *
     * @param tags is an array of tags <b>ordered by position field</b>.
     */
    public void setTags(Tag[] tags) {
        this.tags = tags;
    }

    public void addAnnotations(Set<Annotation> annotations) {
        if (this.annotations == null)
            this.annotations = new HashMap<>(annotations.size());

        for (Annotation annotation : annotations)
            this.annotations.put(annotation.getId(), annotation);
    }

    public void addAnnotation(Annotation annotation) {
        if (annotations == null)
            annotations = new HashMap<>();
        annotations.put(annotation.getId(), annotation);
    }

    public boolean hasAnnotation(String annotation) {
        return getAnnotation(annotation) != null;
    }

    public boolean hasAnnotation(Annotation annotation) {
        return getAnnotation(annotation.getId()) != null;
    }

    public Annotation getAnnotation(String string) {
        return annotations == null ? null : this.annotations.get(string);
    }

    private static String combineSpace(String leftSpace, String rightSpace) {
        if (leftSpace == null)
            return rightSpace;

        if (rightSpace == null)
            return leftSpace;

        //both leftSpace and rightSpace are not null

        //leftSpace is equal to rightSpace
        if (leftSpace.equals(rightSpace))
            return leftSpace;

        //leftSpace is different from rightSpace
        return leftSpace + rightSpace;
    }

    /* return the the space obtained as a combination of the previousSpace and the spaces surrounding Token*/
    public static String combineSpace(String previousSpace, Token token) {
        String space = combineSpace(previousSpace, token.getRightSpace());
        space = combineSpace(space, token.getLeftSpace());
        return space;
    }

    public static String getSpaceBetweenTokens(Token leftToken, Token rightToken) {
        return getSpaceBetweenTokens(null, leftToken, rightToken);
    }

    public static String getSpaceBetweenTokens(String previousSpace, Token leftToken, Token rightToken) {
        String space = combineSpace(previousSpace, leftToken.getRightSpace());
        if (leftToken instanceof Tag) {
            if (rightToken instanceof Tag) {
                //Tag-Tag
                space = combineSpace(space, rightToken.getLeftSpace());
            } else {
                //Tag-Word
                if (!((Word) rightToken).isLeftSpaceRequired())
                    space = null;
            }
        } else {
            if (rightToken instanceof Tag) {
                //Word-Tag
                if (!((Word) leftToken).isRightSpaceRequired())
                    space = null;
                else
                    space = rightToken.getLeftSpace();
            } else {
                //Word-Word
                space = combineSpace(space, rightToken.getLeftSpace());
                if (space == null && (leftToken.isVirtualRightSpace() || rightToken.isVirtualLeftSpace() )) {
                    space = " ";
                }
            }
        }
        return space;
    }


    @Override
    public String toString() {
        return toString(true, false);
    }

    public String toString(boolean printTags, boolean printPlaceholders) {
        return printTags ? toXMLString(printPlaceholders) : toXMLStrippedString(printPlaceholders);
    }

    private String toXMLStrippedString(boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        boolean firstWordFound = false;
        String space = null;

        Token previousToken = null;
        for (Token token : this) {
            if (previousToken != null)
                space = Sentence.getSpaceBetweenTokens(space, previousToken, token);
            if (token instanceof Word) {
                if (firstWordFound) {
                    if (space == null) {
                        space = ((Word) token).isLeftSpaceRequired() ? " " : "";
                    }
                    builder.append(space);
                    space = null;
                }

                String text = printPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                builder.append(text);

                firstWordFound = true;
            }

            previousToken = token;
        }

        return builder.toString();
    }

    private String toXMLString(boolean printPlaceholders) {
        StringBuilder builder = new StringBuilder();

        for (Token token : this) {
            if (token instanceof Tag) {
                builder.append(token.getText());
            } else {
                String text = printPlaceholders || !token.hasText() ? token.getPlaceholder() : token.getText();
                builder.append(XMLUtils.escapeText(text));
            }

            if (token.hasRightSpace())
                builder.append(token.getRightSpace());
        }

        return builder.toString();
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
