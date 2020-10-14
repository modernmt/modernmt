package eu.modernmt.processing.tags.projection;

import eu.modernmt.model.*;


public class TagProjector {

    public Translation project(Translation translation) {
        Sentence source = translation.getSource();
        System.out.println("SOURCE:" + source);

        System.out.println("SOURCE WORDS");
        int i = 0;
        for (Word word : source.getWords())
            System.out.println("i:" + i++ + " word:" + word);

        System.out.println("TARGET WORDS");
        i=0;
        for (Word word : translation.getWords())
            System.out.println("i:" + i++ + " word:" + word);
        if (source.hasTags()) {
            TagCollection sourceTags = new TagCollection(source.getTags(), true);
            System.out.println("SOURCE TAGS");
            for (Tag tag : sourceTags)
                System.out.println(tag + " " + " type:" + tag.getType());

            if (source.hasWords()) {
                if (translation.hasAlignment()) {
                    sourceTags.fixXmlCompliance();

                    Word[] sourceWords = source.getWords();
                    Word[] translationWords = translation.getWords();
                    TagCollection translationTags = new TagCollection();
                    SpanCollection sourceSpans = new SpanCollection(sourceTags.getTags(), sourceWords.length);
                    System.out.println("SOURCE SPANS");
                    for (Span span : sourceSpans)
                        System.out.println(span);


                    SpanTree sourceTree = new SpanTree(sourceSpans);
                    sourceTree.create();
                    System.out.println("SOURCE TREE\n" + sourceTree);
                    Alignment alignment = new Alignment(translation.getWordAlignment(), sourceWords.length, translationWords.length);
                    System.out.println("ALIGNMENT\n" + translation.getWordAlignment());

                    SpanCollection translationSpans = new SpanCollection();
                    translationSpans.project(sourceSpans, alignment, translationWords.length);
                    System.out.println("TARGET SPANS");
                    for (Span span : translationSpans)
                        System.out.println(span);

                    SpanTree translationTree = new SpanTree(translationSpans);
                    translationTree.project(sourceTree, sourceSpans);
//                    System.out.println("TARGET TREE\n" + translationTree);
                    translationTree.sort();
                    System.out.println("TARGET TREE\n" + translationTree);

                    translationTags.populate(translationTree);

                    translation.setTags(translationTags.toArray());
                    simpleSpaceAnalysis(translation);
                }
            } else { //there are no source words; just copy the source tags in the target tags
                translation.setTags(sourceTags.toArray());
            }
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
                        //This Word requires a space on the left,
                        //but no Token between the last Word and this Word has any space (ex. "previousWord<tag1><tag2>thisWord");
                        //hence force a space before this Word
                        if (spaceAfterPreviousWord == null && ((Word) currentToken).isLeftSpaceRequired() && !((Word) currentToken).hasHiddenLeftSpace()) {
                            previousToken.setRightSpace(" ");
                            currentToken.setLeftSpace(" ");
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

}
