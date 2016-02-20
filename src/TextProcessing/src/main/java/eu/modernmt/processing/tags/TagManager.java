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

    private void setAdditionalInfoInMappinTags(MappingTag[] tags, int sourceLength) {
        //identify tag type according to tag text
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].isEmptyTag()) {
                tags[i].setLink(tags[i]);
                tags[i].setContent(false);
                tags[i].getCoveredPositions().add(tags[i].getPosition());
            } else if (tags[i].isOpeningTag()) {//look for corresponding ending tag
                int j = i + 1;
                while (j < tags.length) {
                    if (tags[j].getName().equals(tags[i].getName()) && tags[j].isClosingTag() && tags[i].isOpeningTag()) {
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
                    // artificially covering all words until the end of source
                    if (tags[i].getPosition() < sourceLength) {
                        tags[i].setContent(true);

                        for (int h = tags[i].getPosition(); h < sourceLength; h++) {
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
/*
        for (MappingTag sourceMappingTag : tags) {
            System.out.println("sourceMappingTag: " + sourceMappingTag);
        }
*/
    }

    public void remap(Sentence source, Translation translation) {
        Tag[] sourceTags = source.getTags();
        MappingTag[] sourceMappingTags = new MappingTag[sourceTags.length];
        for (int i = 0; i < sourceMappingTags.length; i++) {
            sourceMappingTags[i] = MappingTag.fromTag(sourceTags[i]);
        }

        setAdditionalInfoInMappinTags(sourceMappingTags, source.getTokens().length);

        setTranslationTags(sourceMappingTags, source, translation);
    }

    private void setTranslationTags(MappingTag[] sourceMappingTags, Sentence source, Translation translation) {
        //create a map from source positions to target position
        ArrayList<ArrayList<Integer>> alignmentSourceToTarget = new ArrayList<>(source.getTokens().length);
        setAlignmentMap(alignmentSourceToTarget, source.getTokens().length, translation.getAlignment());

        ArrayList<MappingTag> targetMappingTags = new ArrayList<>(sourceMappingTags.length);
        for (MappingTag currentSourceMappingTag : sourceMappingTags) {
            /**create the list of target positions associated to this source tag
             * computed by projecting ssource positions ito target postions by means of the word alignment
             */
            ArrayList<Integer> targetPositions = new ArrayList<>();
            HashSet<Integer> targetPositionsSet = new HashSet<>();
            for (int sourceposition : currentSourceMappingTag.getCoveredPositions()) {
                targetPositionsSet.addAll(alignmentSourceToTarget.get(sourceposition));
            }

            targetPositions.addAll(targetPositionsSet);
            Collections.sort(targetPositions);

//            System.out.println("sourcePositions:" + currentSourceMappingTag.getCoveredPositions() + " --> targetPositions:" + targetPositions);

            MappingTag newTargetMappingTag = currentSourceMappingTag.clone();
            // set the position of the target tag
            // it is possible that the set of target positions is empty; in this c

            if (targetPositions.size() > 0) {
                newTargetMappingTag.setPosition(targetPositions.get(0));
            } else {
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
                /* else { //do nothing; there is no gaps } */
            }
        }

        //duplicate MappingTags having gaps in the covered positions
        Collections.sort(targetMappingTags);

        /*
        for (MappingTag currentTargetMappingTag : targetMappingTags) {
            System.out.println("currentTargetMappingTag:" + currentTargetMappingTag);
        }
        System.out.println();
*/

        // transform all target MaappingTags into Tags
        ArrayList<Tag> targetTagList = new ArrayList<>();
        for (MappingTag currentTargetMappingTag : targetMappingTags) {
            ArrayList<Integer> targetPositions = currentTargetMappingTag.getCoveredPositions();

            //System.out.println("\ncurrentTargetMappingTag:" + currentTargetMappingTag);

            int targetPosition = 0;
            if (currentTargetMappingTag.getContent()) { //for tags with content

                Tag targetTag = currentTargetMappingTag.clone();
                if (targetTag.isOpeningTag()) {
                    targetPosition = targetPositions.get(0);
                } else if (targetTag.isClosingTag()) {
                    targetPosition = targetPositions.get(targetPositions.size() - 1) + 1;
                }

            } else { // for EMPTY_TAG and other tags without context
                if (targetPositions.size() > 0 ) {
                    targetPosition = targetPositions.get(0);
                }else { //where to put it?
                    if (currentTargetMappingTag.isClosingTag()) {
                        targetPosition = translation.getTokens().length;
                    } else {
                        targetPosition = 0;
                    }
                }
            }
            Tag targetTag = currentTargetMappingTag.clone();
            targetTag.setPosition(targetPosition);

            //System.out.println("modified targetTag:" + targetTag + "\n");
            targetTagList.add(targetTag);
        }

        /* sort the target Tag list */
        Collections.sort(targetTagList);

        /* transform the target Tag list into an array */
        Tag[] targetTags = new Tag[targetTagList.size()];
        for (int i = 0; i < targetTagList.size(); i++) {
            targetTags[i] = targetTagList.get(i).clone();
        }

        translation.setTags(targetTags);

    }

    protected void setAlignmentMap(ArrayList<ArrayList<Integer>> alignmentMap, int sourceLength, int[][] alignments) {

        //add an empty list for each source word, so that there is a correspondence between source word position and index in the alignmentMap
        for (int i = 0; i < sourceLength; i++) {
            alignmentMap.add(new ArrayList<>());
        }
        for (int[] positionPair : alignments) {
            ArrayList<Integer> currentList = alignmentMap.get(positionPair[0]);
            currentList.add(positionPair[1]);
        }

        System.out.println("ALIGNMENT (Src2Trg):     " + alignmentMap.toString());
        System.out.println();
    }

    public static void main(String[] args) throws Throwable {
        Sentence source = new Sentence(new Token[]{
                new Token("Ciao", true),
                new Token("Davide", true),
                new Token("!", false),
        }, new Tag[]{
                new Tag("c", "<c>", true, false, 0, Tag.Type.OPENING_TAG),
                new Tag("b", "<b id=\"ciao\">", true, false, 1, Tag.Type.OPENING_TAG),
                new Tag("d", "<d/>", true, false, 1, Tag.Type.EMPTY_TAG),
                new Tag("b", "</b>", true, false, 2, Tag.Type.CLOSING_TAG),
                new Tag("f", "<f>", false, false, 2, Tag.Type.OPENING_TAG),
                new Tag("f", "</f>", false, false, 2, Tag.Type.CLOSING_TAG),
                new Tag("e", "<e>", false, false, 3, Tag.Type.OPENING_TAG),
                new Tag("g", "</g>", false, false, 3, Tag.Type.CLOSING_TAG),
        });

        Translation translation = new Translation(new Token[]{
                new Token("Davide", true),
                new Token("Caroselli", false),
                new Token(",", true),
                new Token("Ciao", false),
                new Token("!", true),
        }, source, new int[][]{
                {0, 3},
                {1, 0},
                {1, 1},
                {2, 4},
        });

        System.out.println("SRC:                     " + source);
        System.out.println("SRC (stripped):          " + source.getStrippedString());
        System.out.println();


        new TagManager().remap(source, translation);

        System.out.println("TRANSLATION:             " + translation);
        System.out.println("TRANSLATION (stripped):  " + translation.getStrippedString());
    }
}
