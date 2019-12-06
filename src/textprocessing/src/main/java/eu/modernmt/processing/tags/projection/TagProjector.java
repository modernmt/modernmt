package eu.modernmt.processing.tags.projection;

import eu.modernmt.model.*;

import java.util.Arrays;


public class TagProjector {

    public Translation project(Translation translation) {
        Sentence source = translation.getSource();

        if (source.hasTags()) {
            TagCollection sourceTags = new TagCollection(source.getTags());

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
                    translationTree.project(sourceTree, alignment, translationWords.length);

                    translationTags.populate(translationTree);

                    translation.setTags(translationTags.getTags());
                    simpleSpaceAnalysis(translation);
                }
            } else { //there are no source words; just copy the source tags in the target tags
                Tag[] copy = Arrays.copyOf(sourceTags.getTags(), sourceTags.size());
                translation.setTags(copy);
            }
        }

        return translation;
    }

    public static void simpleSpaceAnalysis(Sentence sentence) {

        Token previousToken = null;
        for (Token currentToken : sentence) {

            if (previousToken == null) {
                if (currentToken instanceof Tag) {
                    //Remove first whitespace of the tag in the first position, only ig it is a Tag
                    currentToken.setLeftSpace(null);
                }
            } else {
                String space = Sentence.getSpace(previousToken, currentToken);
                previousToken.setRightSpace(space);
                currentToken.setLeftSpace(space);
            }
            previousToken = currentToken;
        }
        //Remove the last whitespace whatever token is
        if (previousToken != null) {
            previousToken.setRightSpace(null);
        }

    }

    public static void simpleSpaceAnalysis(Translation translation) {

        Token previousToken = null;
        for (Token currentToken : translation) {

            if (previousToken == null) {
                if (currentToken instanceof Tag) {
                    //Remove first whitespace of the tag in the first position, only ig it is a Tag
                    currentToken.setLeftSpace(null);
                }
            } else {
                String space = Sentence.getSpace(previousToken, currentToken);
                previousToken.setRightSpace(space);
                currentToken.setLeftSpace(space);
            }
            previousToken = currentToken;
        }
        //Remove the last whitespace whatever token is
        if (previousToken != null) {
            previousToken.setRightSpace(null);
        }

    }

}
