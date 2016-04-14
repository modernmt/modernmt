package eu.modernmt.processing.xml;

import eu.modernmt.model.*;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.io.IOException;
import java.util.*;

/**
 * Created by lucamastrostefano on 4/04/16.
 */
public class New_XMLTagMapper implements TextProcessor<Translation, Void> {

    private static class TokenNotFoundException extends Exception {

        public TokenNotFoundException() {
        }
    }

    @Override
    public Void call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        Sentence source = translation.getSource();
        if (source.hasTags()) {
            if (source.hasWords()) {
                if (translation.hasAlignment()) {
                    Tag[] translationTags = mapTags(source.getTags(), source.getWords(),
                            translation.getWords(), translation.getAlignment());
                    translation.setTags(translationTags);
                }
            } else {
                Tag[] tags = source.getTags();
                Tag[] copy = Arrays.copyOf(tags, tags.length);
                translation.setTags(copy);
            }
        }

        return null;
    }

    public static Tag[] mapTags(Tag[] sourceTags, Token[] sourceWord, Token[] targetToken, int[][] alignmetns) {
        ArrayList<Tag> translationTags = new ArrayList<>(sourceTags.length);

        Map<Tag, Integer> tag2order = new HashMap<Tag, Integer>();

        Set<Integer> sourceLeftToken = new HashSet<>();
        Set<Integer> sourceRightToken = new HashSet<>();
        Set<Integer> targetLeftToken = new HashSet<>();
        Set<Integer> targetRightToken = new HashSet<>();
        Set<Integer> leftTokenIntersection = new HashSet<>();
        Set<Integer> rightTokenIntersection = new HashSet<>();

        Set<Integer> alignedTags = new HashSet<Integer>();
        //USE THE ID or pos OF THE TAG INSTEAD OF THE TAG IT-SELF FOR CHECK IF IT HAS BEEN ADDED
        for (int tagIndex = 0; tagIndex < sourceTags.length; tagIndex++) {
            Tag sourceTag = sourceTags[tagIndex];
            //System.out.println(alignedTags);
            if (alignedTags.contains(tagIndex)) {
                continue;
            }
            //System.out.println("TAG: " + sourceTag);
            int sourcePosition = sourceTag.getPosition();
            int closingTagIndex = getClosingTagIndex(sourceTag, tagIndex, sourceTags);
            boolean singleTag = false;
            if (closingTagIndex != -1) {
                Tag closingTag = sourceTags[closingTagIndex];
                int closePosition = closingTag.getPosition();
                int minPos = Integer.MAX_VALUE;
                int maxPos = -1;
                for (int[] align : alignmetns) {
                    if (align[0] >= sourcePosition && align[0] < closePosition) {
                        minPos = Math.min(minPos, align[1]);
                        maxPos = Math.max(maxPos, align[1]);
                    }
                }
                if (minPos == Integer.MAX_VALUE || maxPos == -1) {
                    singleTag = true;
                } else {
                    maxPos += 1;
                    //System.out.println(sourceTag + " " + closingTag + " " + minPos + " " + maxPos);
                    Tag targetTag = Tag.fromTag(sourceTag);
                    targetTag.setPosition(minPos);
                    tag2order.put(targetTag, tagIndex);
                    translationTags.add(targetTag);

                    Tag closingTargetTag = Tag.fromTag(closingTag);
                    closingTargetTag.setPosition(maxPos);
                    tag2order.put(closingTargetTag, closingTagIndex);
                    translationTags.add(closingTargetTag);

                    alignedTags.add(tagIndex);
                    alignedTags.add(closingTagIndex);
                }
            } else {
                singleTag = true;
            }

            if (singleTag) {
                sourceLeftToken.clear();
                sourceRightToken.clear();
                //Compute token alignments
                for (int[] align : alignmetns) {
                    //System.out.print(Arrays.toString(align));
                    if (align[0] < sourcePosition) {
                        if (!sourceRightToken.contains(align[1])) {
                            sourceLeftToken.add(align[1]);
                        }
                    } else {
                        if (!sourceLeftToken.contains(align[1])) {
                            sourceRightToken.add(align[1]);
                        }
                    }
                }
                //System.out.println();
                //System.out.println("LEFT SOURCE TOKENS: " + sourceLeftToken);
                //System.out.println("INNER SOURCE TOKENS: " + sourceInnerToken);
                //System.out.println("RIGHT SOURCE TOKENS: " + sourceRightToken);

                //Find best configuration
                targetLeftToken.clear();
                targetRightToken.clear();
                leftTokenIntersection.clear();
                rightTokenIntersection.clear();
                for (int i = 0; i < targetToken.length; i++) {
                    targetRightToken.add(i);
                }
                rightTokenIntersection.addAll(sourceRightToken);
                rightTokenIntersection.retainAll(targetRightToken);
                //double maxScore = rightTokenIntersection.size()/ Math.max(1, targetRightToken.size());
                int maxScore = rightTokenIntersection.size();
                int bestPosition = 0;
                //System.out.println("LEFT: " + targetLeftToken + " ||| " + leftTokenIntersection);
                //System.out.println("INNER: " + targetInnerToken + " ||| " + innerTokenIntersection);
                //System.out.println("RIGHT: " + targetRightToken + " ||| " + rightTokenIntersection);
                //System.out.println(bestPosition + " " + maxScore);
                //System.out.println("-----------");
                int actualPosition = 0;
                Tag[] tagsArray = new Tag[translationTags.size()];
                for (int i = 0; i < targetToken.length; i++) {
                    actualPosition++;
                    //System.out.println(targetOpeningPosition);
                    targetLeftToken.add(i);
                    targetRightToken.remove(i);
                    leftTokenIntersection.clear();
                    rightTokenIntersection.clear();
                    leftTokenIntersection.addAll(sourceLeftToken);
                    leftTokenIntersection.retainAll(targetLeftToken);
                    rightTokenIntersection.addAll(sourceRightToken);
                    rightTokenIntersection.retainAll(targetRightToken);
                    //double score = leftTokenIntersection.size() / Math.max(1, targetLeftToken.size()) + innerTokenIntersection.size() * 2 / Math.max(1, targetInnerToken.size()) + rightTokenIntersection.size() / Math.max(1, targetRightToken.size());
                    int score = leftTokenIntersection.size() + rightTokenIntersection.size();
                    //score *= 2;
                    //score -= (targetLeftToken.size() - leftTokenIntersection.size()) + (targetInnerToken.size() - innerTokenIntersection.size()) * 2 + (targetRightToken.size() - rightTokenIntersection.size());
                    //System.out.println("LEFT: " + targetLeftToken + " ||| " + leftTokenIntersection);
                    //System.out.println("INNER: " + targetInnerToken + " ||| " + innerTokenIntersection);
                    //System.out.println("RIGHT: " + targetRightToken + " ||| " + rightTokenIntersection);
                    //System.out.println(actualPosition + " " + score);
                    //System.out.println("-----------");
                    if (score > maxScore) {
                        maxScore = score;
                        bestPosition = actualPosition;
                    } else {
                        //break;
                    }
                }
                //System.out.println("Best pos:" + bestPosition + "\n\n");
                Tag targetTag = Tag.fromTag(sourceTag);
                targetTag.setPosition(bestPosition);
                tag2order.put(targetTag, tagIndex);
                translationTags.add(targetTag);
                alignedTags.add(tagIndex);
            }
        }
        Collections.sort(translationTags, new Comparator<Tag>() {
            @Override
            public int compare(Tag tag1, Tag tag2) {
                int c = tag1.compareTo(tag2);
                if (c == 0) {
                    c = tag2order.get(tag1) - tag2order.get(tag2);
                }
                return c;
            }
        });
        //System.out.println(translationTags);
        Tag[] result = new Tag[translationTags.size()];
        return translationTags.toArray(result);
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
        int minIndex = tagIndex;
        int open = 1;
        for (int index = 0; index < tags.length; index++) {
            Tag tag = tags[index];
            if (index < minIndex || openingTag == tag) {
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
        //throw new TokenNotFoundException();
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }

    public static void main(String[] args) throws Throwable {
        // SRC: hello <b>world</b><f />!
        Sentence source = new Sentence(new Word[]{
                new Word("View", " "),
                new Word("your", " "),
                new Word("files", " "),
                new Word("from", " "),
                new Word("any", " "),
                new Word("computer", " "),
                new Word("connected", " "),
                new Word("to", " "),
                new Word("the", " "),
                new Word("Internet", " "),
                new Word(",", " "),
                new Word("from", " "),
                new Word("your", " "),
                new Word("smartphone", " "),
                new Word("and", " "),
                new Word("tablet", " "),
                new Word("by", " "),
                new Word("downloading", " "),
                new Word("the", " "),
                new Word("iPhone", " "),
                new Word(",", " "),
                new Word("iPad", " "),
                new Word(",", " "),
                new Word("Android", " "),
                new Word("or", " "),
                new Word("BlackBerry", " "),
                new Word("app", null),
                new Word(".", null)
        }, new Tag[]{
                Tag.fromText("<a>", true, " ", 4),
                Tag.fromText("<a>", true, " ", 10),
                Tag.fromText("<a>", true, " ", 20),
                Tag.fromText("<a>", true, " ", 21),
                Tag.fromText("<a>", true, " ", 22),
                Tag.fromText("<a>", true, " ", 23),
                Tag.fromText("<a>", true, " ", 24),
                Tag.fromText("<a>", true, " ", 25),
                Tag.fromText("<a>", true, " ", 26),
                Tag.fromText("<a>", true, " ", 28),


        });
        Translation translation = new Translation(new Word[]{
                new Word("Visualizza", " "),
                new Word("i", " "),
                new Word("tuoi", " "),
                new Word("file", " "),
                new Word("da", " "),
                new Word("qualsiasi", " "),
                new Word("computer", " "),
                new Word("connesso", " "),
                new Word("a", " "),
                new Word("internet", null),
                new Word(",", " "),
                new Word("dal", " "),
                new Word("tuo", " "),
                new Word("smartphone", " "),
                new Word("e", " "),
                new Word("dal", " "),
                new Word("tablet", " "),
                new Word("scaricando", " "),
                new Word("le", " "),
                new Word("applicazioni", " "),
                new Word("per", " "),
                new Word("iPhone", " "),
                new Word(",", " "),
                new Word("iPad", " "),
                new Word(",", " "),
                new Word("Android", " "),
                new Word("e", " "),
                new Word("BlackBerry", null),
                new Word(".", null),
        }, source, new int[][]{
                {0, 0},
                {1, 2},
                {2, 1},
                {2, 3},
                {3, 4},
                {4, 5},
                {5, 6},
                {6, 7},
                {7, 8},
                {9, 9},
                {10, 10},
                {11, 11},
                {12, 12},
                {13, 13},
                {14, 14},
                {15, 16},
                {16, 17},
                {17, 17},
                {18, 18},
                {19, 20},
                {19, 21},
                {20, 22},
                {21, 23},
                {22, 22},
                {22, 24},
                {23, 25},
                {25, 27},
                {26, 19},
                {27, 28}
        });


        System.out.println("SRC:                     " + source);
        System.out.println("SRC (stripped):          " + source.getStrippedString(false));
        System.out.println();

        New_XMLTagMapper mapper = new New_XMLTagMapper();
        mapper.call(translation, null);

        System.out.println("TRANSLATION:             " + translation);
        System.out.println("TRANSLATION (stripped):  " + translation.getStrippedString(false));
    }
}


/**
 * Created by davide on 17/02/16.
 */
