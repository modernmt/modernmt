package eu.modernmt.processing.tags;

import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.*;

import java.util.ArrayList;

/**
 * Created by davide on 17/02/16.
 */
public class TagManager {

    protected void setAdditionalInfoInMappinTags(_mappingTag[] tags, int sourceLength) {
        //pattern for  tag name, i.e. the longest string before a space or before the ending backslash '/'
//        String tag_name_pattern = "[a-zA-Z]";
        Pattern tagNamePattern = compile("[a-zA-Z]");

        //pattern for  tag checking ending tag, i.e. tag like </name>
//        String closingTagPattern = "</[a-zA-Z]";
        Pattern closingTagPattern = compile("</[a-zA-Z]");

        //pattern for  tag checking self-contained tag, i.e. tag like <name />
//        String selfContainedTagPattern = "/>";
        Pattern selfContainedTagPattern = compile("/>");

        boolean[] closingTagFlag = new boolean[tags.length];
        boolean[] selfContainedTagFlag = new boolean[tags.length];

        String _name;
        for (int i = 0; i < tags.length; i++) {
            Matcher matcher = tagNamePattern.matcher(tags[i].text);
            _name = (matcher.find()) ? matcher.group(0) : "";
            tags[i].setName(_name);
            matcher = selfContainedTagPattern.matcher(tags[i].text);
            selfContainedTagFlag[i] = matcher.find();

            matcher = closingTagPattern.matcher(tags[i].text);
            closingTagFlag[i] = matcher.find();
        }

        //identify tag type according to tag text
        for (int i = 0; i < tags.length; i++) {
            // check whether the current tag is still undefined
            if (tags[i].getType() == _mappingTag.Type.UNDEF) {

                // check whether the current tag is SELF_CONTAINED
                if (selfContainedTagFlag[i]) {
                    tags[i].setType(_mappingTag.Type.SELF_CONTAINED);
                    tags[i].setLink(i);
                    tags[i].setParent(i);
                    tags[i].getCoveredPositions().add(tags[i].getPosition());
                    continue;
                } else {
                    int j = i + 1;
                    while (j < tags.length) {
                        if (tags[j].getName().equals(tags[i].getName()) && closingTagFlag[j] && !closingTagFlag[i]) {

                            // check whether the current tag is either OPENED/CLOSED_EMPTY/NONEMPT_TEXT or CONTAINS_NONEMPTY_TEXT
                            if (tags[j].getPosition() == tags[i].getPosition()) {
                                tags[i].setType(_mappingTag.Type.OPENED_EMPTY_TEXT);
                                tags[j].setType(_mappingTag.Type.CLOSED_EMPTY_TEXT);
                            } else {
                                tags[i].setType(_mappingTag.Type.OPENED_NONEMPTY_TEXT);
                                tags[j].setType(_mappingTag.Type.CLOSED_NONEMPTY_TEXT);
                            }
                            tags[i].setLink(j);
                            tags[j].setLink(i);
                            tags[i].setParent(i);
                            tags[j].setParent(i);
                            for (int h = tags[i].getPosition(); h < tags[j].getPosition(); h++) {
                                tags[i].getCoveredPositions().add(h);
                                tags[j].getCoveredPositions().add(h);
                            }
                            break;
                        }
                        j++;
                    }
                    if (j == tags.length) {
                        // check whether the current tag is either OPENED_BUT_UNCLOSED or CLOSED_BUT_UNOPENED
                        if (!closingTagFlag[i]) {
                            tags[i].setType(_mappingTag.Type.OPENED_BUT_UNCLOSED);
                            tags[i].setParent(i);
                            // artificially covering all words until the end of source
                            for (int h = tags[i].getPosition(); h < sourceLength; h++) {
                                tags[i].getCoveredPositions().add(h);
                            }
                        } else {
                            tags[i].setType(_mappingTag.Type.CLOSED_BUT_UNOPENED);
                            tags[i].setParent(i);
                            // artificially covering all words from the beginning of the source
                            for (int h = 0; h < tags[i].getPosition(); h++) {
                                tags[i].getCoveredPositions().add(h);
                            }
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

    public void remap(_Sentence source, _Translation translation) {
        _mappingTag[] sourceMappingTags = new _mappingTag[source.getTags().length];
        _Tag[] sourceTags = source.getTags();
        for (int i = 0; i < sourceMappingTags.length; i++) {
            sourceMappingTags[i] = _mappingTag.fromTag(sourceTags[i]);
        }

        setAdditionalInfoInMappinTags(sourceMappingTags, source.getTokens().length);

        setTranslationTags(sourceMappingTags, source, translation);
    }

    protected void setTranslationTags(_mappingTag[] sourceMappingTags, _Sentence source, _Translation translation) {

        //create a map from source positions to target position
        ArrayList<ArrayList<Integer>> alignmentSourceToTarget = new ArrayList<>(source.getTokens().length);
        setAlignmentMap(alignmentSourceToTarget, source.getTokens().length, translation.getAlignment());

        System.out.println("Alignment (SourceToTarget):" + alignmentSourceToTarget.toString());
        System.out.println();

        ArrayList<_mappingTag> targetMappingTags = new ArrayList<>(sourceMappingTags.length);
        for (int i = 0; i < sourceMappingTags.length; i++) {
            _mappingTag currentSourceMappingTag = sourceMappingTags[i];
            _mappingTag.Type currentSourceTagType = currentSourceMappingTag.getType();
            /**check whether the source position associated to this tag is associated with any word (i.e.position != -1);
             * if not just add the tag with the same info in the target
             */
            _mappingTag newTargetMappingTag = new _mappingTag(currentSourceMappingTag.getText(), currentSourceMappingTag.hasLeftSpace(), currentSourceMappingTag.hasRightSpace(), -1, currentSourceTagType, currentSourceMappingTag.getName(), currentSourceMappingTag.getLink());
            ArrayList<Integer> targetPositions = new ArrayList<>();
            if (currentSourceTagType == _mappingTag.Type.SELF_CONTAINED) {
                /** check the type pof source tag and act consequently
                 * if SELF_CONTAINED put it as it is in the aligned position (note that link should be 0)
                 * else if OPENED/CLOSED_EMPTY_TEXT put it as it is in the aligned position of the opening tag (note that this tag and linked tag should points to the same source word)
                 * else if OPENED/CLOSED_NONEMPTY_TEXT || OPENED_BUT_UNCLOSED || CLOSED_BUT_UNOPENED associate a tag for each target word associated to the source words spanned
                 * */
                ArrayList<Integer> sourcePositions = currentSourceMappingTag.getCoveredPositions();
                HashSet<Integer> targetPositionsSet = new HashSet<>();
                for (int j = 0; j < sourcePositions.size(); j++) {
                    targetPositionsSet.addAll(alignmentSourceToTarget.get(sourcePositions.get(j)));
                }

                targetPositions.addAll(targetPositionsSet);
                Collections.sort(targetPositions);
            } else if (currentSourceTagType == _mappingTag.Type.OPENED_EMPTY_TEXT || currentSourceMappingTag.getType() == _mappingTag.Type.CLOSED_EMPTY_TEXT) {
                targetPositions.addAll(alignmentSourceToTarget.get(currentSourceMappingTag.getPosition()));
            } else {
                ArrayList<Integer> sourcePositions = currentSourceMappingTag.getCoveredPositions();
                HashSet<Integer> targetPositionsSet = new HashSet<>();
                for (int j = 0; j < sourcePositions.size(); j++) {
                    targetPositionsSet.addAll(alignmentSourceToTarget.get(sourcePositions.get(j)));
                }

                targetPositions.addAll(targetPositionsSet);
                Collections.sort(targetPositions);
            }
            newTargetMappingTag.setCoveredPositions(targetPositions);
            newTargetMappingTag.setParent(currentSourceMappingTag.getParent());
            targetMappingTags.add(newTargetMappingTag);
        }

        Collections.sort(targetMappingTags);
/*
        for (int i = 0; i < targetMappingTags.size(); i++) {
            _mappingTag currentTargetMappingTag = targetMappingTags.get(i);
            System.out.println("i:" + i + " currentTargetMappingTag text:" + currentTargetMappingTag.getText()
                    + " type:" + currentTargetMappingTag.getType() + " position:" + currentTargetMappingTag.getPosition()
                    + " parent:" + currentTargetMappingTag.getParent() + " link:" + currentTargetMappingTag.getLink()
                    + " positions:" + currentTargetMappingTag.getCoveredPositions().toString());

        }
*/
        ArrayList<_Tag> targetTagList = new ArrayList<>();
        for (int i = 0; i < targetMappingTags.size(); i++) {
            _mappingTag currentTargetMappingTag = targetMappingTags.get(i);
            _mappingTag.Type currentTargetTagType = currentTargetMappingTag.getType();
            ArrayList<Integer> targetPositions = currentTargetMappingTag.getCoveredPositions();

            //System.out.println("currentTargetMappingTag.text" + currentTargetMappingTag.getText() + " targetPositions: " + targetPositions.toString());
            if (targetPositions.size() == 0) {
//do nothing
            } else if (targetPositions.size() == 1) {

                int firstTargetPosition = targetPositions.get(0);
                int targetPosition = firstTargetPosition;

                if (currentTargetTagType == _mappingTag.Type.CLOSED_NONEMPTY_TEXT || currentTargetTagType == _mappingTag.Type.CLOSED_BUT_UNOPENED) {
                    targetPosition = firstTargetPosition + 1;
                } else {
                    //do nothing; i.e. i.e. keep firstTargetPosition
                }
                //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), targetPosition);

                _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);

                targetTag.setPosition(targetPosition);

                targetTagList.add(targetTag);
                //System.out.println("targetTag text:" + targetTag.getText() + " position:" + targetPosition);
            } else {
                int firstTargetPosition = targetPositions.get(0);
                int lastTargetPosition = targetPositions.get(targetPositions.size() - 1);
                int targetPosition = firstTargetPosition;
                if (currentTargetTagType == _mappingTag.Type.SELF_CONTAINED) {
                    targetPosition = lastTargetPosition;
                    //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), lastTargetPosition);
                    _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);
                    targetTag.setPosition(targetPosition);
                    targetTagList.add(targetTag);

                    System.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());
                } else if (currentTargetTagType == _mappingTag.Type.OPENED_BUT_UNCLOSED) {
                    //do nothing; i.e. i.e. keep firstTargetPosition
                    //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), firstTargetPosition);
                    _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);
                    targetTag.setPosition(targetPosition);
                    targetTagList.add(targetTag);
                } else if (currentTargetTagType == _mappingTag.Type.CLOSED_BUT_UNOPENED) {
                    targetPosition = lastTargetPosition + 1;
                    //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), lastTargetPosition + 1);
                    _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);
                    targetTag.setPosition(targetPosition);
                    targetTagList.add(targetTag);
                } else if (currentTargetTagType == _mappingTag.Type.OPENED_EMPTY_TEXT || currentTargetMappingTag.getType() == _mappingTag.Type.CLOSED_EMPTY_TEXT) {
                    targetPosition = lastTargetPosition;
                    //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), lastTargetPosition);
                    _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);
                    targetTag.setPosition(targetPosition);
                    targetTagList.add(targetTag);
                } else if (currentTargetTagType == _mappingTag.Type.OPENED_NONEMPTY_TEXT) {
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
                            //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), startPosition);
                            _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);
                            targetTag.setPosition(startPosition);
                            targetTagList.add(targetTag);
                            //System.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());

                            firstIndex = nextIndex;
                            nextIndex = firstIndex + 1;
                        }

                    }
                    //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), startPosition);
                    _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);
                    targetTag.setPosition(startPosition);
                    targetTagList.add(targetTag);
                    //Syastem.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());


                } else if (currentTargetTagType == _mappingTag.Type.CLOSED_NONEMPTY_TEXT) {
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
                            //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), startPosition);
                            _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);
                            targetTag.setPosition(lastPosition + 1);
                            targetTagList.add(targetTag);
                            //System.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());

                            firstIndex = nextIndex;
                            nextIndex = firstIndex + 1;
                        }

                    }
                    //_Tag targetTag = new _Tag(currentTargetMappingTag.getText(), currentTargetMappingTag.hasLeftSpace(), currentTargetMappingTag.hasRightSpace(), startPosition);
                    _Tag targetTag = _Tag.fromTag(currentTargetMappingTag);
                    targetTag.setPosition(lastPosition + 1);
                    targetTagList.add(targetTag);
                    //Syastem.out.println("targetTag text:" + targetTag.getText() + " position:" + targetTag.getPosition());

                } else {
                    //should never enter here, becase it means that  currentTargetTagType == _mappingTag.Type.UNDEF
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

        _Tag[] targetTags = new _Tag[targetTagList.size()];
        for (int i = 0; i < targetTagList.size(); i++) {
            targetTags[i] = _Tag.fromTag(targetTagList.get(i));
        }
        translation.setTags(targetTags);

    }

    protected void setAlignmentMap(ArrayList<ArrayList<Integer>> alignmentMap, int sourceLength, int[][] alignments) {

        //add an empty list for each source word, so that there is a correspondence between source word position and index in the alignmentMap
        for (int i = 0; i < sourceLength; i++) {
            alignmentMap.add(new ArrayList<>());
        }
        for (int i = 0; i < alignments.length; i++) {
            ArrayList<Integer> currentList = alignmentMap.get(alignments[i][0]);
            currentList.add(alignments[i][1]);
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
        _Sentence source = new _Sentence(new _Token[]{
                new _Token("Ciao", true),
                new _Token("Davide", true),
                new _Token("!", false),
        }, new _Tag[]{
                new _Tag("<c>", true, false, 0),
                new _Tag("<b>", true, false, 1),
                new _Tag("<d/>", true, false, 1),
                new _Tag("</e>", true, false, 2),
                new _Tag("</b>", true, false, 2),
        });

        _Translation translation = new _Translation(new _Token[]{
                new _Token("Hello", true),
                new _Token("Davide", false),
                new _Token("!", false),
        }, source, new int[][]{
                {0, 0},
                {2, 1},
                {1, 2},
        });


        System.out.println(source);
        System.out.println(source.getStrippedString());
        System.out.println();


        new TagManager().remap(source, translation);
        System.out.println(translation);
        System.out.println(translation.getStrippedString());
    }
}
