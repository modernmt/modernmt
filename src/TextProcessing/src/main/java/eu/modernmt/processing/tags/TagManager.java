package eu.modernmt.processing.tags;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Created by davide on 17/02/16.
 */
public class TagManager {

    private static void setAdditionalInfoInMappinTags(MappingTag[] tags, int sourceLength) {
        //identify tag type according to tag text
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].isEmptyTag()) {
                tags[i].setLink(tags[i]);
                tags[i].setParent(i);
                tags[i].getCoveredPositions().add(tags[i].getPosition());
            } else {
                int j;
                for (j = i + 1; j < tags.length; j++) {
                    if (tags[j].getName().equals(tags[i].getName()) && tags[j].isClosingTag() && tags[i].isOpeningTag()) {

                        tags[i].setLink(tags[j]);
                        tags[j].setLink(tags[i]);
                        tags[i].setParent(i);
                        tags[j].setParent(i);
                        for (int h = tags[i].getPosition(); h < tags[j].getPosition(); h++) {
                            tags[i].getCoveredPositions().add(h);
                            tags[j].getCoveredPositions().add(h);
                        }
                        break;
                    }
                }

                if (j == tags.length) {
                    // check whether the current tag is either OPENED_BUT_UNCLOSED or CLOSED_BUT_UNOPENED
                    if (tags[i].isOpeningTag()) {
                        tags[i].setParent(i);
                        // artificially covering all words until the end of source
                        for (int h = tags[i].getPosition(); h < sourceLength; h++) {
                            tags[i].getCoveredPositions().add(h);
                        }
                    } else {
                        tags[i].setParent(i);
                        // artificially covering all words from the beginning of the source
                        for (int h = 0; h < tags[i].getPosition(); h++) {
                            tags[i].getCoveredPositions().add(h);
                        }
                    }
                }
            }
        }

/*
        for (int i = 0; i < tags.length; i++) {
            System.out.println("i:" + i + " sourceMappingTag text:" + tags[i].getText() + " type:" + tags[i].getType() + " link:" + tags[i].getLink()
                    + " parent:" + tags[i].getParent() + " position:" + tags[i].getPosition() + " coveredPosition:" + tags[i].getCoveredPositions().toString());
        }
*/
    }

    public static void remap(Sentence source, Translation translation) {
        MappingTag[] sourceMappingTags = new MappingTag[source.getTags().length];
        Tag[] sourceTags = source.getTags();
        for (int i = 0; i < sourceMappingTags.length; i++) {
            sourceMappingTags[i] = MappingTag.fromTag(sourceTags[i]);
        }

        setAdditionalInfoInMappinTags(sourceMappingTags, source.getTokens().length);

        setTranslationTags(sourceMappingTags, source, translation);
    }

    private static void setTranslationTags(MappingTag[] sourceMappingTags, Sentence source, Translation translation) {
        //create a map from source positions to target position
        ArrayList<ArrayList<Integer>> alignmentSourceToTarget = new ArrayList<>(source.getTokens().length);
        setAlignmentMap(alignmentSourceToTarget, source.getTokens().length, translation.getAlignment());

        // System.out.println("Alignment (SourceToTarget):" + alignmentSourceToTarget.toString());
        // System.out.println();

        ArrayList<MappingTag> targetMappingTags = new ArrayList<>(sourceMappingTags.length);
        for (MappingTag currentSourceMappingTag : sourceMappingTags) {
            MappingTag.Type currentSourceTagType = currentSourceMappingTag.getType();

            /**check whether the source position associated to this tag is associated with any word (i.e.position != -1);
             * if not just add the tag with the same info in the target
             */
            MappingTag newTargetMappingTag = MappingTag.fromTag(currentSourceMappingTag);
            newTargetMappingTag.setPosition(-1);

            ArrayList<Integer> targetPositions = new ArrayList<>();

            if (currentSourceMappingTag.isOpenedEmpty() || currentSourceMappingTag.isClosedEmpty()) {
                targetPositions.addAll(alignmentSourceToTarget.get(currentSourceMappingTag.getPosition()));
            } else {
                ArrayList<Integer> sourcePositions = currentSourceMappingTag.getCoveredPositions();
                HashSet<Integer> targetPositionsSet = new HashSet<>();

                for (int sourceposition : sourcePositions) {
                    targetPositionsSet.addAll(alignmentSourceToTarget.get(sourceposition));
                }

                targetPositions.addAll(targetPositionsSet);
                Collections.sort(targetPositions);
            }

//            if (currentSourceMappingTag.isEmptyTag()) {
//                /** check the type pof source tag and act consequently
//                 * if SELF_CONTAINED put it as it is in the aligned position (note that link should be 0)
//                 * else if OPENED/CLOSED_EMPTY_TEXT put it as it is in the aligned position of the opening tag (note that this tag and linked tag should points to the same source word)
//                 * else if OPENED/CLOSED_NONEMPTY_TEXT || OPENED_BUT_UNCLOSED || CLOSED_BUT_UNOPENED associate a tag for each target word associated to the source words spanned
//                 * */
//                ArrayList<Integer> sourcePositions = currentSourceMappingTag.getCoveredPositions();
//                HashSet<Integer> targetPositionsSet = new HashSet<>();
//
//                for (int sourceposition : sourcePositions) {
//                    targetPositionsSet.addAll(alignmentSourceToTarget.get(sourceposition));
//                }
//
//                targetPositions.addAll(targetPositionsSet);
//                Collections.sort(targetPositions);
//            } else if (currentSourceMappingTag.isOpenedEmpty() || currentSourceMappingTag.isClosedEmpty()) {
//                targetPositions.addAll(alignmentSourceToTarget.get(currentSourceMappingTag.getPosition()));
//            } else {
//                ArrayList<Integer> sourcePositions = currentSourceMappingTag.getCoveredPositions();
//                HashSet<Integer> targetPositionsSet = new HashSet<>();
//
//                for (int sourceposition : sourcePositions) {
//                    targetPositionsSet.addAll(alignmentSourceToTarget.get(sourceposition));
//                }
//
//                targetPositions.addAll(targetPositionsSet);
//                Collections.sort(targetPositions);
//            }
            newTargetMappingTag.setCoveredPositions(targetPositions);
            newTargetMappingTag.setParent(currentSourceMappingTag.getParent());
            targetMappingTags.add(newTargetMappingTag);
        }

        Collections.sort(targetMappingTags);
/*
        for (int i = 0; i < targetMappingTags.size(); i++) {
            MappingTag currentTargetMappingTag = targetMappingTags.get(i);
            System.out.println("i:" + i + " currentTargetMappingTag text:" + currentTargetMappingTag.getText()
                    + " type:" + currentTargetMappingTag.getType() + " position:" + currentTargetMappingTag.getPosition()
                    + " parent:" + currentTargetMappingTag.getParent() + " link:" + currentTargetMappingTag.getLink()
                    + " positions:" + currentTargetMappingTag.getCoveredPositions().toString());

        }
*/
        ArrayList<Tag> targetTagList = new ArrayList<>();
        for (MappingTag currentTargetMappingTag : targetMappingTags) {
            ArrayList<Integer> targetPositions = currentTargetMappingTag.getCoveredPositions();

            //System.out.println("currentTargetMappingTag.text" + currentTargetMappingTag.getText() + " targetPositions: " + targetPositions.toString());
            if (targetPositions.size() == 0) {
//do nothing
            } else if (targetPositions.size() == 1) {

                int firstTargetPosition = targetPositions.get(0);
                int targetPosition = firstTargetPosition;

                //if (currentTargetTagType == MappingTag.Type.CLOSED_NONEMPTY_TEXT || currentTargetTagType == MappingTag.Type.CLOSED_BUT_UNOPENED) {
                if (currentTargetMappingTag.isClosedNonEmpty() || currentTargetMappingTag.isClosedButUnopend()) {
                    targetPosition = firstTargetPosition + 1;
                } else {
                    //do nothing; i.e. i.e. keep firstTargetPosition
                }
                //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), targetPosition);

                Tag targetTag = currentTargetMappingTag.clone();

                targetTag.setPosition(targetPosition);

                targetTagList.add(targetTag);
                //System.out.println("targetTag text:" + targetTag.getText() + " position:" + targetPosition);
            } else {
                int firstTargetPosition = targetPositions.get(0);
                int lastTargetPosition = targetPositions.get(targetPositions.size() - 1);
                int targetPosition = firstTargetPosition;
                //if (currentTargetTagType == MappingTag.Type.SELF_CONTAINED) {
                if (currentTargetMappingTag.isEmptyTag()) {
                    targetPosition = lastTargetPosition;
                    //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), lastTargetPosition);
                    Tag targetTag = currentTargetMappingTag.clone();
                    targetTag.setPosition(targetPosition);
                    targetTagList.add(targetTag);

                    System.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());
//                } else if (currentTargetTagType == MappingTag.Type.OPENED_BUT_UNCLOSED) {
                } else if (currentTargetMappingTag.isOpenedButUncloed()) {
                    //do nothing; i.e. i.e. keep firstTargetPosition
                    //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), firstTargetPosition);
                    Tag targetTag = currentTargetMappingTag.clone();
                    targetTag.setPosition(targetPosition);
                    targetTagList.add(targetTag);
//                } else if (currentTargetTagType == MappingTag.Type.CLOSED_BUT_UNOPENED) {
                } else if (currentTargetMappingTag.isClosedButUnopend()) {
                    targetPosition = lastTargetPosition + 1;
                    //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), lastTargetPosition + 1);
                    Tag targetTag = currentTargetMappingTag.clone();
                    targetTag.setPosition(targetPosition);
                    targetTagList.add(targetTag);
                    //} else if (currentTargetTagType == MappingTag.Type.OPENED_EMPTY_TEXT || currentTargetMappingTag.getType() == MappingTag.Type.CLOSED_EMPTY_TEXT) {
                } else if (currentTargetMappingTag.isOpenedEmpty() || currentTargetMappingTag.isClosedEmpty()) {
                    targetPosition = lastTargetPosition;
                    //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), lastTargetPosition);
                    Tag targetTag = currentTargetMappingTag.clone();
                    targetTag.setPosition(targetPosition);
                    targetTagList.add(targetTag);
//                } else if (currentTargetTagType == MappingTag.Type.OPENED_NONEMPTY_TEXT) {
                } else if (currentTargetMappingTag.isOpenedNonEmpty()) {
                    int firstIndex = 0;
                    int nextIndex = firstIndex + 1;
                    int targetPositionsSize = targetPositions.size();
                    int startPosition = targetPositions.get(firstIndex);
                    int lastPosition;
                    while (nextIndex < targetPositionsSize) {
                        startPosition = targetPositions.get(firstIndex);
                        lastPosition = targetPositions.get(nextIndex);
                        if (lastPosition - startPosition == nextIndex - firstIndex) { //if positions are contiguous
                            nextIndex++;
                        } else {
                            //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), startPosition);
                            Tag targetTag = currentTargetMappingTag.clone();
                            targetTag.setPosition(startPosition);
                            targetTagList.add(targetTag);
                            //System.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());

                            firstIndex = nextIndex;
                            nextIndex = firstIndex + 1;
                        }

                    }
                    //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), startPosition);
                    Tag targetTag = currentTargetMappingTag.clone();
                    targetTag.setPosition(startPosition);
                    targetTagList.add(targetTag);
                    //Syastem.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());


                    //} else if (currentTargetTagType == MappingTag.Type.CLOSED_NONEMPTY_TEXT) {
                } else if (currentTargetMappingTag.isClosedNonEmpty()) {
                    int firstIndex = 0;
                    int nextIndex = firstIndex + 1;
                    int targetPositionsSize = targetPositions.size();
                    int startPosition;
                    int lastPosition = targetPositions.get(nextIndex);
                    while (nextIndex < targetPositionsSize) {
                        startPosition = targetPositions.get(firstIndex);
                        lastPosition = targetPositions.get(nextIndex);
                        if (lastPosition - startPosition == nextIndex - firstIndex) { //if positions are contiguous
                            nextIndex++;
                        } else {
                            //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), startPosition);
                            Tag targetTag = currentTargetMappingTag.clone();
                            targetTag.setPosition(lastPosition + 1);
                            targetTagList.add(targetTag);
                            //System.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());

                            firstIndex = nextIndex;
                            nextIndex = firstIndex + 1;
                        }

                    }
                    //Tag targetTag = new Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), startPosition);
                    Tag targetTag = currentTargetMappingTag.clone();
                    targetTag.setPosition(lastPosition + 1);
                    targetTagList.add(targetTag);
                    //Syastem.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());

                } else {
                    //should never enter here, becase it means that  currentTargetTagType == MappingTag.Type.UNDEF
                    //do nothing
                }
            }
        }

        Collections.sort(targetTagList);

/*
        System.out.println("after sorting");
        for (int i = 0; i < targetTagList.size(); i++) {
            System.out.println("targetTagList i:" + i + " text:" + targetTagList.get(i).getText() + " position:" + targetTagList.get(i).getPosition());
        }
*/

        Tag[] targetTags = new Tag[targetTagList.size()];
        for (int i = 0; i < targetTagList.size(); i++) {
            targetTags[i] = targetTagList.get(i).clone();
        }
        translation.setTags(targetTags);

    }

    protected static void setAlignmentMap(ArrayList<ArrayList<Integer>> alignmentMap, int sourceLength, int[][] alignments) {

        //add an empty list for each source word, so that there is a correspondence between source word position and index in the alignmentMap
        for (int i = 0; i < sourceLength; i++) {
            alignmentMap.add(new ArrayList<>());
        }
        /*
        for (int i = 0; i < alignments.length; i++) {
            ArrayList<Integer> currentList = alignmentMap.get(alignments[i][0]);
            currentList.add(alignments[i][1]);
        }
        */
        for (int[] positionPair : alignments) {
            ArrayList<Integer> currentList = alignmentMap.get(positionPair[0]);
            currentList.add(positionPair[1]);
        }


/*
        System.out.println("alignmentMap size:" + alignmentMap.size() + " ");
        for (int i = 0; i < alignmentMap.size(); i++) {
            ArrayList<Integer> currentList = alignmentMap.get(i);
            for (int j = 0; j < currentList.size(); j++) {
                System.out.println("i:" + i + " currentList:" + currentList.toString());
            }
        }
        System.out.println();
        */

    }

    public static void main(String[] args) throws Throwable {
        // hello <f/> <b>world</b> <world />

        Sentence source = new Sentence(new Token[]{
                new Token("hello", true),
                new Token("world", true),
        }, new Tag[]{
                new Tag("f", "<f>", true, true, 1, Tag.Type.EMPTY_TAG),
                new Tag("b", "<b>", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("b", "</b>", false, true, 2, Tag.Type.CLOSING_TAG),
                new Tag("world", "<world />", true, false, 2, Tag.Type.EMPTY_TAG),
        });

        Translation translation = new Translation(new Token[]{
                new Token("ciao", true),
                new Token("mondo", true),
        }, source, new int[][]{
                {0, 0},
                {1, 1},
        });


        System.out.println(source);
        System.out.println(source.getStrippedString());
        System.out.println();


        TagManager.remap(source, translation);
        System.out.println(translation);
        System.out.println(translation.getStrippedString());
    }
}
