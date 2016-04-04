package eu.modernmt.processing.xml;

import eu.modernmt.model.*;
import eu.modernmt.processing.framework.TextProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Created by davide on 17/02/16.
 */


/**
 * Policy for tag management
 * From class Tag a tag is classified into EMPTY_TAG, OPENING_TAG, CLOSING_TAG, and has  aposition associated corresponding to the word immediately following the tag itself. If the tag closes the sentence its position is set to a virtual last word (i.e. position=sentence.length)
 * <p>
 * MappingTag add additional information:
 * - a "link" to the closing or opening corresponding tag;
 * - a list of "coveredPositions"; for a source MappingTags this list contains only contiguous positions, or it is empty; for target tags this list can contain also non contiguous positions, or it is empty
 * - a boolean "content" flag which is set to true if at least one source word is contained between the opening and closing tags; this flag is set by looking at the source sentence, and reported to the mapped target MappingTag
 * <p>
 * For an opening source tag without the corresponding closing tag, the set of covered positions goes from the its actual position to the end of the sentence
 * For a closing source tag without the corresponding opening tag, the set of covered positions goes from the beginning of the sentence to its actual position
 * <p>
 * A MappingTag can be:
 * - with context (content=true): at least one word is contained between the opening and closing tag
 * - without context (content=false): no words are contained between the opening and closing tag:
 * <p>
 * In the case of a MappingTag without content the list of "coveredPositions" represents only the word before which the tags whould be positioned
 * <p>
 * The mapping from a source MappingTag into a target MappingTag is done as follows:
 * - all additional info are copied, but the list of "coveredPositions"
 * - the list of source "coveredPositions" is scanned; considering the provided source-to-target word-alignment, for each source position the corresponding target positions (if any) are orderly and uniquely inserted in the target list of "coveredPositions"
 * - a target MappingTag which has a list of "coveredPositions" with internal gaps is split into similar copies containing contiguous "coveredPositions"
 * - the "position" of the target MappingTag is re-set according to the rules/heuristic described below, which determines where the tag is actually re-inserted
 * <p>
 * Rules of insertions:
 * - rules for tags without content
 * -- the tag is inserted before the first element of the (target) "coveredPositions"; if the list is empty the tag is inserted at the end of the sentence; this holds for both EMPTY_TAG, OPENING_TAG and CLOSING_TAG;
 * - rules for tags with content
 * -- if the tag is an OPENING_TAG, the tag is inserted before the first element of the (target) "coveredPositions"
 * -- if the tag is a CLOSING_TAG, the tag is inserted before the last element of the (target) "coveredPositions"
 * -- in both cases, if the list is empty the tag is inserted at the beginning of the sentence;
 */
public class XMLTagMapper implements TextProcessor<Translation, Void> {

    @Override
    public Void call(Translation translation) {
        Sentence source = translation.getSource();

        if (source.hasTags()) {
            if (source.hasWords()) {
                if (translation.hasAlignment()) {
                    remap(translation);
                    restoreTagSpacing(translation);
                }
            } else {
                Tag[] tags = source.getTags();
                Tag[] copy = new Tag[tags.length];

                for (int i = 0; i < tags.length; i++)
                    copy[i] = Tag.fromTag(tags[i]);

                translation.setTags(copy);
            }
        }

        return null;
    }

    @Override
    public void close() {
        // Nothing to do
    }

    public static void remap(Translation translation) {
        Sentence source = translation.getSource();
        Tag[] sourceTags = source.getTags();
        MappingTag[] sourceMappingTags = new MappingTag[sourceTags.length];
        for (int i = 0; i < sourceMappingTags.length; i++) {
            sourceMappingTags[i] = MappingTag.fromTag(sourceTags[i]);
        }

        setAdditionalInfoInMappinTags(sourceMappingTags, source.getWords().length);
        setTranslationTags(sourceMappingTags, source, translation);
    }

    /**
     * Recreate the spacing informations of the translation including tags, this table
     * shows the behaviour of the algorithm:
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
     */
    public static void restoreTagSpacing(Translation translation) {
        int j = 0;

        Token[] sentence = new Token[translation.length()];
        for (Token token : translation)
            sentence[j++] = token;

        // Set right-space info

        for (int i = 0; i < sentence.length; i++) {
            Token token = sentence[i];

            Tag nextTag = null;
            if (i < sentence.length - 1 && (sentence[i + 1] instanceof Tag))
                nextTag = (Tag) sentence[i + 1];

            if (nextTag != null) {
                boolean isSpacedClosingComment = (nextTag.isComment() && nextTag.isClosingTag() && nextTag.hasLeftSpace());
                boolean mustPrintSpace;

                if (isSpacedClosingComment) {
                    token.setRightSpace(" ");
                    mustPrintSpace = false;
                } else if (!token.hasRightSpace()) {
                    mustPrintSpace = false;
                } else if (nextTag.hasLeftSpace() == nextTag.hasRightSpace()) {
                    if (nextTag.isClosingTag()) {
                        token.setRightSpace(null);
                        mustPrintSpace = true;
                    } else {
                        token.setRightSpace(" ");
                        mustPrintSpace = false;
                    }
                } else if (nextTag.hasLeftSpace()) {
                    token.setRightSpace(" ");
                    mustPrintSpace = false;
                } else {
                    token.setRightSpace(null);
                    mustPrintSpace = true;
                }

                while (nextTag != null) {
                    i++;

                    boolean isSpacedOpeningComment = (nextTag.isComment() && nextTag.isOpeningTag() && nextTag.hasRightSpace());

                    if (isSpacedOpeningComment) {
                        nextTag.setRightSpace(" ");
                        mustPrintSpace = false;
                    } else {
                        nextTag.setRightSpace(null);
                    }

                    if (i < sentence.length - 1 && (sentence[i + 1] instanceof Tag)) {
                        Tag previousTag = nextTag;
                        nextTag = (Tag) sentence[i + 1];

                        isSpacedClosingComment = (nextTag.isComment() && nextTag.isClosingTag() && nextTag.hasLeftSpace());

                        if (isSpacedClosingComment || (mustPrintSpace && !nextTag.isClosingTag())) {
                            previousTag.setRightSpace(" ");
                            mustPrintSpace = false;
                        } else {
                            previousTag.setRightSpace(null);
                        }
                    } else {
                        if (!isSpacedOpeningComment)
                            nextTag.setRightSpace(mustPrintSpace ? " " : null);
                        nextTag = null;
                    }
                }
            } else {
                token.setRightSpace(token.hasRightSpace() && i < sentence.length - 1 ? " " : null);
            }
        }

        // Copy right-space info to left-space

        for (int i = 0; i < sentence.length; i++) {
            Token token = sentence[i];
            if (!(token instanceof Tag))
                continue;

            Tag tag = (Tag) token;

            if (i == 0) {
                tag.setLeftSpace(false);
            } else {
                Token previous = sentence[i - 1];
                tag.setLeftSpace(previous.hasRightSpace());
            }
        }

        // Enforce spacing rules on first and last token
        if (sentence[0] instanceof Tag)
            ((Tag) sentence[0]).setLeftSpace(false);

        sentence[sentence.length - 1].setRightSpace(null);
    }

    private static void setAdditionalInfoInMappinTags(MappingTag[] tags, int sourceLength) {
        //identify tag type according to tag text
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].isEmptyTag()) {
                tags[i].setLink(tags[i]);
                tags[i].setContent(false);
                tags[i].getCoveredPositions().add(tags[i].getPosition());
            } else if (tags[i].isOpeningTag()) {//look for corresponding ending tag
                int j = i + 1;
                while (j < tags.length) {
                    if (tags[j].closes(tags[i])) {
                        tags[i].setLink(tags[j]);
                        tags[j].setLink(tags[i]);
                        if (tags[i].getPosition() == tags[j].getPosition()) {
                            tags[i].setContent(false);
                            tags[i].getCoveredPositions().add(tags[i].getPosition());
                            tags[j].getCoveredPositions().add(tags[i].getPosition());
                        } else {
                            tags[i].setContent(true);
                            for (int h = tags[i].getPosition(); h < tags[j].getPosition(); h++) {
                                tags[i].getCoveredPositions().add(h);
                                tags[j].getCoveredPositions().add(h);
                            }
                        }
                        break;
                    }
                    j++;
                }

                if (j == tags.length) { //there is no corresponding closing tag
                    // artificially covering all words until the end of source (including the virtual last word
                    if (tags[i].getPosition() <= sourceLength) {
                        tags[i].setContent(true);

                        for (int h = tags[i].getPosition(); h <= sourceLength; h++) {
                            tags[i].getCoveredPositions().add(h);
                        }
                    }
                    /* else {// do nothing; content is false by default } */
                }
            } else {//closing_tag
                if (!tags[i].hasLink()) { //this closing tag has no corresponding opening tag
                    // artificially covering all words from the beginning of the source
                    if (tags[i].getPosition() > 0) {
                        tags[i].setContent(true);
                        for (int h = 0; h < tags[i].getPosition(); h++) {
                            tags[i].getCoveredPositions().add(h);
                        }
                    }
                    /* else { // do nothing; content is false by default } */
                } else {
                    tags[i].setContent(tags[i].getLink().getContent());
                }
            }
        }

//        for (MappingTag sourceMappingTag : tags) {
//            System.out.println("sourceMappingTag: " + sourceMappingTag);
//        }

    }

    private static void setTranslationTags(MappingTag[] sourceMappingTags, Sentence source, Translation translation) {
        //create a map from source positions to target position
        ArrayList<ArrayList<Integer>> alignmentSourceToTarget = new ArrayList<>(source.getWords().length);
        setAlignmentMap(alignmentSourceToTarget, source.getWords().length, translation.getWords().length, translation.getAlignment());

        ArrayList<MappingTag> targetMappingTags = new ArrayList<>(sourceMappingTags.length);
        for (MappingTag currentSourceMappingTag : sourceMappingTags) {
            // create the list of target positions associated to this source tag
            // computed by projecting ssource positions ito target postions by means of the word alignment
            ArrayList<Integer> targetPositions = new ArrayList<>();
            HashSet<Integer> targetPositionsSet = new HashSet<>();

            for (int sourceposition : currentSourceMappingTag.getCoveredPositions()) {
                targetPositionsSet.addAll(alignmentSourceToTarget.get(sourceposition));
            }


            targetPositions.addAll(targetPositionsSet);
            Collections.sort(targetPositions);

            //System.out.println("sourcePositions:" + currentSourceMappingTag.getCoveredPositions() + " --> targetPositions:" + targetPositions);

            MappingTag newTargetMappingTag = currentSourceMappingTag.clone();
            // set the position of the target tag
            // it is possible that the set of target positions is empty; in this c

            if (targetPositions.size() > 0) {
                newTargetMappingTag.setPosition(targetPositions.get(0));
            } else {
                newTargetMappingTag.setContent(false);
                newTargetMappingTag.setPosition(0);
            }

            newTargetMappingTag.setCoveredPositions(targetPositions);
            newTargetMappingTag.setPosition(-1); // set position undefined
            targetMappingTags.add(newTargetMappingTag);

            //duplicate MappingTags having gaps in the covered positions
            ArrayList<Integer> currentPositions = newTargetMappingTag.getCoveredPositions();
            if (currentPositions.size() > 1) {
                int firstIndex = 0;
                int nextIndex = firstIndex + 1;
                int lastIndex = currentPositions.size();

                int firstTargetPosition = currentPositions.get(firstIndex);

                if ((currentPositions.get(lastIndex - 1) - firstTargetPosition + 1) != (lastIndex - firstIndex)) {

                    ArrayList<Integer> newPositions = new ArrayList<>();
                    newPositions.add(firstTargetPosition);

                    int nextTargetPosition;
                    while (nextIndex < lastIndex) {
                        nextTargetPosition = currentPositions.get(nextIndex);

                        if ((nextTargetPosition - firstTargetPosition) > (nextIndex - firstIndex)) { //there is a gap

                            MappingTag additionalTargetMappingTag = newTargetMappingTag.clone();
                            additionalTargetMappingTag.setCoveredPositions(newPositions);
                            targetMappingTags.add(additionalTargetMappingTag);

                            firstIndex = nextIndex;
                            nextIndex = firstIndex + 1;
                            newPositions = new ArrayList<>();
                            firstTargetPosition = currentPositions.get(firstIndex);
                            newPositions.add(firstTargetPosition);
                        } else { //consecutive position
                            newPositions.add(nextTargetPosition);
                            nextIndex++;
                        }
                    }
                    //replacing the covered position of the current MappingTag
                    newTargetMappingTag.setCoveredPositions(newPositions);
                }
                /* else { //do nothing; there are no gaps } */
            }
        }

        //duplicate MappingTags having gaps in the covered positions
        Collections.sort(targetMappingTags);

//        for (MappingTag currentTargetMappingTag : targetMappingTags) {
//            System.out.println("currentTargetMappingTag:" + currentTargetMappingTag + " content " + currentTargetMappingTag.getContent() + " positions:" + currentTargetMappingTag.getCoveredPositions());
//        }


        // transform all target MaappingTags into Tags
        ArrayList<Tag> targetTagList = new ArrayList<>();
        for (MappingTag currentTargetMappingTag : targetMappingTags) {
            ArrayList<Integer> targetPositions = currentTargetMappingTag.getCoveredPositions();

            int targetPosition = 0;
            if (currentTargetMappingTag.getContent()) { //for tags with content

                Tag targetTag = currentTargetMappingTag.clone();
                if (targetTag.isOpeningTag()) {
                    targetPosition = targetPositions.get(0);
                } else if (targetTag.isClosingTag()) {
                    targetPosition = targetPositions.get(targetPositions.size() - 1) + 1;
                }

            } else { // for EMPTY_TAG and other tags without context
                if (targetPositions.size() > 0) {
                    targetPosition = targetPositions.get(0);
                } else { //where to put it?
                    // heuristic: if tag has no content and no covered positions, put at the end of the sentence
                    targetPosition = 0;
                    /*  other possible heuristic:
                     *  if tag has no content and no covered positions, put at the end of the sentence
                     *  targetPosition = translation.getWords().length;
                     */
                }
            }
            Tag targetTag = currentTargetMappingTag.clone();
            targetTag.setPosition(targetPosition);

            targetTagList.add(targetTag);
        }

        /* sort the target Tag list */
        Collections.sort(targetTagList);

        /* transform the target Tag list into an array */
        Tag[] targetTags = new Tag[targetTagList.size()];
        for (int i = 0; i < targetTagList.size(); i++) {
            targetTags[i] = Tag.fromTag(targetTagList.get(i));
        }

        translation.setTags(targetTags);

    }

    protected static void setAlignmentMap(ArrayList<ArrayList<Integer>> alignmentMap, int sourceLength, int targetLength, int[][] alignments) {
        /** add an empty list for each source word,
         * so that there is a correspondence between source word position and index in the alignmentMap
         * a space is reserved for a virtual last word at the end of the source sentence to handle tags positioned at the end of the sentence
         */
        for (int i = 0; i <= sourceLength; i++) {
            alignmentMap.add(new ArrayList<>());
        }
        for (int[] positionPair : alignments) {
            ArrayList<Integer> currentList = alignmentMap.get(positionPair[0]);
            currentList.add(positionPair[1]);
        }

        /** addition of a link between the virtual last words of source and target sentences,
         * this link is added to handle tags positioned at the end of the sentences
         * */
        ArrayList<Integer> currentList = alignmentMap.get(sourceLength);
        currentList.add(targetLength);

//        System.out.println("ALIGNMENT (Src2Trg):     " + alignmentMap);
    }

    public static void main(String[] args) throws Throwable {
        // SRC: hello <b>world</b><f />!
        Sentence source = new Sentence(new Word[]{
                new Word("hello", " "),
                new Word("world", null),
                new Word("!", null),
        }, new Tag[]{
                Tag.fromText("<b>", true, null, 1),
                Tag.fromText("</b>", false, " ", 2),
                Tag.fromText("<f/>", false, null, 2),
        });
        Translation translation = new Translation(new Word[]{
                new Word("mondo", " "),
                new Word("ciao", null),
                new Word("!", null),
        }, source, new int[][]{
                {0, 1},
                {1, 0},
                {2, 2},
        });


        System.out.println("SRC:                     " + source);
        System.out.println("SRC (stripped):          " + source.getStrippedString(false));
        System.out.println();

        XMLTagMapper.remap(translation);

        System.out.println("TRANSLATION:             " + translation);
        System.out.println("TRANSLATION (stripped):  " + translation.getStrippedString(false));
    }
}
