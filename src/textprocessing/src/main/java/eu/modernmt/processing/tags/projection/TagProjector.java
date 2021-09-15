package eu.modernmt.processing.tags.projection;

import eu.modernmt.model.*;

import java.util.regex.Pattern;

public class TagProjector {

    public Translation project(Translation translation) {
        Sentence source = translation.getSource();

        if (source.hasTags()) {
            TagCollection sourceTags = new TagCollection(source.getTags(), true);

            if (source.hasWords()) {
                if (translation.hasAlignment()) {
                    sourceTags.fixXmlCompliance();

                    Word[] sourceWords = source.getWords();
                    Word[] translationWords = translation.getWords();
                    TagCollection translationTags = new TagCollection();
                    SpanCollection sourceSpans = new SpanCollection(sourceTags.getTags(), sourceWords.length);

                    SpanTree sourceTree = new SpanTree(sourceSpans);
                    sourceTree.create();

                    Alignment alignment = new Alignment(translation.getWordAlignment(), sourceWords.length, translationWords.length);

                    SpanCollection translationSpans = new SpanCollection();
                    translationSpans.project(sourceSpans, alignment, translationWords.length);

                    SpanTree translationTree = new SpanTree(translationSpans);
                    translationTree.project(sourceTree, sourceSpans);
                    translationTree.sort();
                    translationTags.populate(translationTree);

                    translation.setTags(translationTags.toArray());
                    simpleSpaceAnalysis(translation);
                }
            } else { //there are no source words; just copy the source tags in the target tags
                translation.setTags(sourceTags.toArray());
            }
        }

        //ad-hoc fixings

        //ISSUE: newlines are sometimes put before a puntuation mark
        //in these case, we move punctuation mark just before the "newline"
        Token previousToken = null;
        for (Token token : translation) {
            if ( (previousToken != null)
                && (previousToken instanceof Tag)
                && (((Tag) previousToken).getType() == Tag.Type.SEPARATOR_TAG)
                && (token instanceof Word)
                && isPunctuation((Word) token) ) {
                ((Tag) previousToken).setPosition(((Tag) previousToken).getPosition() + 1);
                token.setRightSpace(previousToken.getLeftSpace());
            }
            previousToken = token;
        }

        return translation;
    }

    public static void simpleSpaceAnalysis(Sentence sentence) {

        int wordN = sentence.getWords().length;
        int wordIdx = 0;
        boolean lastWord = false;

        String spaceAfterPreviousWord = null;
        Token previousToken = null;
        String space;
        for (Token currentToken : sentence) {

            if (previousToken == null) {
                if (currentToken instanceof Tag) {
                    //Remove first whitespace of the tag in the first position, only if it is a Tag
                    currentToken.setLeftSpace(null);
                }
            } else {
                if (wordIdx == 0) { //first Word
                    space = previousToken.getRightSpace();
                } else if (lastWord && previousToken instanceof Word) { //last Word
                    space = currentToken.getLeftSpace();
                } else {
                    space = Sentence.getSpaceBetweenTokens(previousToken, currentToken);
                }

                previousToken.setRightSpace(space);
                currentToken.setLeftSpace(space);


                if (currentToken instanceof Tag) { // X-Tag
                    spaceAfterPreviousWord = Sentence.combineSpace(spaceAfterPreviousWord, currentToken);
                } else { // X-Word
                    if (previousToken instanceof Tag) {// Tag-Word
                        if (((Tag) previousToken).getType() != Tag.Type.SEPARATOR_TAG) {
                            //This Word requires a space on the left,
                            //but no Token between the last Word and this Word has any space (ex. "previousWord<tag1><tag2>thisWord");
                            //hence force a space before this Word
                            if (spaceAfterPreviousWord == null && ((Word) currentToken).isLeftSpaceRequired() && !((Word) currentToken).hasHiddenLeftSpace()) {
                                previousToken.setRightSpace(" ");
                                currentToken.setLeftSpace(" ");
                            }
                        }
                    }
                    spaceAfterPreviousWord = null;

                }

                if (!(currentToken instanceof Tag))
                    wordIdx++;
                if (wordIdx == wordN - 1)
                    lastWord = true;
            }
            previousToken = currentToken;
        }
        //Remove the last whitespace whatever token is
        if (previousToken != null) {
            previousToken.setRightSpace(null);
        }

    }

    private static Pattern PUNCTUATION_WORD = Pattern.compile("^[.,;:?!,.?;:!！？｡：；，？、。：;！.!،؛؟:]+$", Pattern.UNICODE_CHARACTER_CLASS);

    public static boolean isPunctuation(Word word) {
        return PUNCTUATION_WORD.matcher(word.getPlaceholder()).matches();
    }
}
