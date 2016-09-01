package eu.modernmt.processing.xml;

import eu.modernmt.model.*;

import java.util.*;

/**
 * Created by lucamastrostefano on 4/04/16.
 */
public class XMLTagProjector {

    private static class ExtendedTag implements Comparable<ExtendedTag> {

        private Tag tag;
        private int sourcePosition;
        private int sourceTagIndex;
        private int targetTagIndex;
        private int targetPosition;

        public ExtendedTag(Tag tag, int sourcePosition, int sourceTagIndex, int targetPosition) {
            this.tag = tag;
            this.sourcePosition = sourcePosition;
            this.sourceTagIndex = sourceTagIndex;
            this.targetPosition = targetPosition;
        }

        @Override
        public int compareTo(ExtendedTag extendedTag) {
            int c = this.tag.compareTo(extendedTag.tag);
            if (c == 0) {
                c = this.sourceTagIndex - extendedTag.sourceTagIndex;
            }
            return c;
        }

        @Override
        public boolean equals(Object o) {
            return this.sourceTagIndex == ((ExtendedTag) o).sourceTagIndex;
        }

        @Override
        public int hashCode() {
            int result = tag != null ? tag.hashCode() : 0;
            result = 31 * result + sourceTagIndex;
            return result;
        }

    }

    public Translation project(Translation translation) {
        Sentence source = translation.getSource();
        if (source.hasTags()) {
            if (source.hasWords()) {
                if (translation.hasAlignment()) {
                    mapTags(translation);
                    simpleSpaceAnalysis(translation);
                }
            } else {
                Tag[] tags = source.getTags();
                Tag[] copy = Arrays.copyOf(tags, tags.length);
                translation.setTags(copy);
            }
        }
        return translation;
    }

    public static List<ExtendedTag> mapTags(Translation translation) {
        Tag[] sourceTags = translation.getSource().getTags();
        Token[] sourceWord = translation.getSource().getWords();
        Token[] targetTokens = translation.getWords();
        Alignment alignment = translation.getAlignment();
        List<ExtendedTag> translationTags = new ArrayList<>(sourceTags.length);
        Map<Integer, Integer> closing2opening = new HashMap<>();

        Set<Integer> sourceLeftToken = new HashSet<>();
        Set<Integer> sourceRightToken = new HashSet<>();
        Set<Integer> targetLeftToken = new HashSet<>();
        Set<Integer> targetRightToken = new HashSet<>();
        Set<Integer> leftTokenIntersection = new HashSet<>();
        Set<Integer> rightTokenIntersection = new HashSet<>();

        Set<Integer> alignedTags = new HashSet<Integer>();

        for (int tagIndex = 0; tagIndex < sourceTags.length; tagIndex++) {
            Tag sourceTag = sourceTags[tagIndex];
            //If the tag has been already mapped (such as well formed closing tags), then continue
            if (alignedTags.contains(tagIndex)) {
                continue;
            }

            int sourcePosition = sourceTag.getPosition();
            boolean singleTag = false;

            int closingTagIndex = getClosingTagIndex(sourceTag, tagIndex, sourceTags);
            //If the current tag has a closing tag
            if (closingTagIndex != -1) {
                Tag closingTag = sourceTags[closingTagIndex];
                int closePosition = closingTag.getPosition();
                int minPos = Integer.MAX_VALUE;
                int maxPos = -1;

                //Check if they contain some aligned words
                for (int[] align : alignment) {
                    if (align[0] >= sourcePosition && align[0] < closePosition) {
                        minPos = Math.min(minPos, align[1]);
                        maxPos = Math.max(maxPos, align[1]);
                    }
                }

                //If they contain no aligned words, treat the current tag as a self-closing tag
                if (minPos == Integer.MAX_VALUE || maxPos == -1) {
                    singleTag = true;
                } else {
                    //Else map both the tags in order to enclose the words aligned
                    // to those contained in the source sentence
                    maxPos += 1;
                    Tag targetTag = Tag.fromTag(sourceTag);
                    targetTag.setPosition(minPos);
                    translationTags.add(new ExtendedTag(targetTag, sourceTag.getPosition(), tagIndex, minPos));

                    Tag closingTargetTag = Tag.fromTag(closingTag);
                    closingTargetTag.setPosition(maxPos);
                    translationTags.add(new ExtendedTag(closingTargetTag, closingTag.getPosition(), closingTagIndex, maxPos));

                    alignedTags.add(tagIndex);
                    alignedTags.add(closingTagIndex);
                }
            } else {
                //If not closing tag has been found, treat this tag as a self-closing tag
                singleTag = true;
            }

            //If it is a self-closing tag
            if (singleTag) {
                sourceLeftToken.clear();
                sourceRightToken.clear();
                //Words that are at the left of the tag in the source sentence, should be at left of the mapped tag
                //in the translation. Some reasoning for those that are at the right.
                for (int[] align : alignment) {
                    //If the word is at the left of the current tag
                    if (align[0] < sourcePosition) {
                        if (!sourceRightToken.contains(align[1])) {
                            //Remember that it should be at the left also in the translation
                            sourceLeftToken.add(align[1]);
                        }
                    } else {
                        //It the word is at the right of the current tag
                        if (!sourceLeftToken.contains(align[1])) {
                            //Remember that it should be at the right also in the translation
                            sourceRightToken.add(align[1]);
                        }
                    }
                }
                boolean openingTag = sourceTag.isOpeningTag();
                //Find the mapped position that respects most of the left-right word-tag relationship as possible.
                targetLeftToken.clear();
                targetRightToken.clear();
                leftTokenIntersection.clear();
                rightTokenIntersection.clear();

                for (int i = 0; i < targetTokens.length; i++) {
                    targetRightToken.add(i);
                }
                rightTokenIntersection.addAll(sourceRightToken);
                rightTokenIntersection.retainAll(targetRightToken);
                int maxScore = rightTokenIntersection.size();
                int bestPosition = 0;

                int actualPosition = 0;
                for (int i = 0; i < targetTokens.length; i++) {
                    actualPosition++;

                    targetLeftToken.add(i);
                    targetRightToken.remove(i);
                    leftTokenIntersection.clear();
                    rightTokenIntersection.clear();

                    leftTokenIntersection.addAll(sourceLeftToken);
                    leftTokenIntersection.retainAll(targetLeftToken);
                    rightTokenIntersection.addAll(sourceRightToken);
                    rightTokenIntersection.retainAll(targetRightToken);
                    int score = leftTokenIntersection.size() + rightTokenIntersection.size();

                    //Remember the best position and score (for opening tag prefer to shift them to the right)
                    if ((openingTag && score >= maxScore) || (!openingTag && score > maxScore)) {
                        maxScore = score;
                        bestPosition = actualPosition;
                    } else if (score < maxScore) {
                        //The function that describes the score should be concave,
                        // so we can break as soon it starts decreasing
                        //break
                    }
                }
                Integer openingPosition = closing2opening.get(tagIndex);
                if (openingPosition != null) {
                    bestPosition = Math.max(openingPosition, bestPosition);
                } else if (closingTagIndex != -1) {
                    closing2opening.put(closingTagIndex, bestPosition);
                }
                //Map the tag to the best position
                Tag targetTag = Tag.fromTag(sourceTag);
                targetTag.setPosition(bestPosition);
                translationTags.add(new ExtendedTag(targetTag, sourceTag.getPosition(), tagIndex, bestPosition));
                alignedTags.add(tagIndex);
            }
        }

        //Sort the tag in according to their position and order in the source sentence
        Collections.sort(translationTags);
        for (int i = 0; i < translationTags.size(); i++) {
            translationTags.get(i).targetTagIndex = i;
        }

        Tag[] result = new Tag[translationTags.size()];
        for (int i = 0; i < translationTags.size(); i++) {
            result[i] = translationTags.get(i).tag;
        }
        translation.setTags(result);

        return translationTags;
    }

    private static int getPositionOpeningTag(Tag closingTag, Tag[] tags) {
        int maxPosition = closingTag.getPosition();
        for (Tag tag : tags) {
            if (closingTag.closes(tag)) {
                return tag.getPosition();
            }
            if (tag.getPosition() >= maxPosition) {
                break;
            }
        }
        return -1;
    }

    private static int getClosingTagIndex(Tag openingTag, int tagIndex, Tag[] tags) {
        int minIndex = tagIndex + 1;
        int open = 1;
        for (int index = minIndex; index < tags.length; index++) {
            Tag tag = tags[index];
            if (openingTag == tag) {
                continue;
            }
            if (openingTag.getName().equals(tag.getName()) && tag.isOpeningTag()) {
                open++;
            }
            if (openingTag.opens(tag)) {
                open--;
                if (open == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    public static void simpleSpaceAnalysis(Translation translation) {

        //Add whitespace between the last word and the next tag if the latter has left space
        Tag[] tags = translation.getTags();
        Word[] words = translation.getWords();
        int firstTagAfterLastWord = tags.length - 1;
        boolean found = false;
        while (firstTagAfterLastWord >= 0 && tags[firstTagAfterLastWord].getPosition() == words.length) {
            firstTagAfterLastWord--;
            found = true;
        }
        if (found) {
            firstTagAfterLastWord++;
            if (tags[firstTagAfterLastWord].hasLeftSpace() && !words[words.length - 1].hasRightSpace()) {
                words[words.length - 1].setRightSpace(" ");
            }
        }

        //Remove whitespace between word and the next tag, if the first has no right space.
        //Left trim first token if it is a tag
        Token previousToken = null;
        for (Token token : translation) {
            if (token instanceof Tag) {
                Tag tag = (Tag) token;
                if (previousToken != null && previousToken.hasRightSpace() && !tag.hasLeftSpace()) {
                    if (!tag.hasRightSpace()) {
                        tag.setRightSpace(previousToken.getRightSpace());
                    }
                    previousToken.setRightSpace(null);
                } else if (previousToken == null) {
                    //Remove first whitespace
                    tag.setLeftSpace(false);
                }
            }
            previousToken = token;

        }
        //Remove the last whitespace
        if (previousToken != null) {
            previousToken.setRightSpace(null);
        }

    }

}