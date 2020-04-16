package eu.modernmt.processing.tags.projection;


import eu.modernmt.model.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class SpanCollection implements Iterable<Span> {

    private static int ROOT_LEVEL = 0;
    private static int ROOT_INDEX = 0;

    private List<Span> list;


    SpanCollection() {
        list = new ArrayList<>();
    }

    SpanCollection(Tag[] tags, int words) {
        list = new ArrayList<>();
        populate(tags, words);
    }

    private void populate(Tag[] tags, int words) {

        Map<String, List<Integer>> openingTagSet = new HashMap<>();
        Map<String, List<Integer>> closingTagSet = new HashMap<>();
        Map<String, List<Integer>> emptyTagSet = new HashMap<>();

        for (int tagIndex = 0; tagIndex < tags.length; tagIndex++) {
            Tag tag = tags[tagIndex];
            String name = tag.getName();
            if (tag.getType() == Tag.Type.OPENING_TAG) {
                openingTagSet.computeIfAbsent(name, k -> new ArrayList<>());
                openingTagSet.get(name).add(tagIndex);
            }
            if (tag.getType() == Tag.Type.CLOSING_TAG) {
                closingTagSet.computeIfAbsent(name, k -> new ArrayList<>());
                closingTagSet.get(tag.getName()).add(tagIndex);
            }
            if (tag.getType() == Tag.Type.EMPTY_TAG) {
                emptyTagSet.computeIfAbsent(name, k -> new ArrayList<>());
                emptyTagSet.get(name).add(tagIndex);
            }

        }

        int[] tagLevel = new int[tags.length];

        int level = ROOT_LEVEL;
        int minLevel = 0;
        for (int t = 0; t < tags.length; t++) {
            Tag.Type type = tags[t].getType();
            if (type == Tag.Type.EMPTY_TAG) {
                //do nothing
                tagLevel[t] = level;
                minLevel = minLevel < level ? minLevel : level;
            } else if (type == Tag.Type.OPENING_TAG) {
                tagLevel[t] = level;
                level++;
                minLevel = minLevel < level ? minLevel : level;
            } else if (type == Tag.Type.CLOSING_TAG) {
                level--;
                tagLevel[t] = level;
                minLevel = minLevel < level ? minLevel : level;
            }
        }
        minLevel--;
        for (int t = 0; t < tagLevel.length; t++) {
            tagLevel[t] = tagLevel[t] - minLevel;
        }

        int[] tagLink = new int[tags.length];
        boolean[] tagVisit = new boolean[tags.length];

        for (String name : openingTagSet.keySet()) {
            List<Integer> openingTags = openingTagSet.get(name);
            List<Integer> closingTags = closingTagSet.get(name);
            if (closingTags == null) {
                // there are no closing tags for this name; hence all opening tags for this name are spurious
                for (Integer beginTagIdx : openingTags) {
                    tagVisit[beginTagIdx] = true;
                    tagLink[beginTagIdx] = -1;
                }
            } else {
                for (int bt = openingTags.size() - 1; bt >= 0; bt--) {
                    int beginTagIdx = openingTags.get(bt);

                    if (tagVisit[beginTagIdx]) {
                        continue;
                    }

                    int endTagIdx = -1;
                    for (int idx : closingTags) {
                        if (!tagVisit[idx] && idx > beginTagIdx) {
                            endTagIdx = idx;
                            break;
                        }
                    }
                    if (endTagIdx != -1) {
                        //found opening/closing pair
                        //create the corresponding span opening/closing span
                        //visit both
                        tagVisit[beginTagIdx] = true;
                        tagVisit[endTagIdx] = true;
                        tagLink[beginTagIdx] = endTagIdx;
                        tagLink[endTagIdx] = beginTagIdx;
                    } else {
                        //found opening tag without closing
                        //create the corresponding span opening span without closing
                        //visit opening tag
                        tagVisit[beginTagIdx] = true;
                        tagLink[beginTagIdx] = -1;

                    }
                }
            }
        }

        for (String name : closingTagSet.keySet()) {
            List<Integer> openingTags = openingTagSet.get(name);
            List<Integer> closingTags = closingTagSet.get(name);
            if (openingTags == null) {
                // there are no opening tags for this name; hence all closing tags for this name are spurious
                for (Integer endTagIdx : closingTags) {
                    tagVisit[endTagIdx] = true;
                    tagLink[endTagIdx] = -1;
                }
            } else {
                for (int endTagIdx : closingTags) {
                    if (tagVisit[endTagIdx]) {
                        continue;
                    }

                    int beginTagIdx = -1;
                    for (int bt = openingTags.size() - 1; bt >= 0; bt--) {
                        int idx = openingTags.get(bt);
                        if (!tagVisit[idx] && idx > endTagIdx) {
                            beginTagIdx = idx;
                            break;
                        }
                    }

                    if (beginTagIdx != -1) {
                        //found opening/closing pair
                        //create the corresponding span opening/closing span
                        //visit both
                        tagVisit[beginTagIdx] = true;
                        tagVisit[endTagIdx] = true;
                        tagLink[beginTagIdx] = endTagIdx;
                        tagLink[endTagIdx] = beginTagIdx;
                    } else {
                        //found closing tag without opening
                        //create the corresponding span closing span without opening
                        //visit closing tag
                        tagVisit[endTagIdx] = true;
                        tagLink[endTagIdx] = -1;
                    }
                }
            }
        }

        for (String name : emptyTagSet.keySet()) {
            for (Integer emptyTagIdx : emptyTagSet.get(name)) {
                tagVisit[emptyTagIdx] = true;
                tagLink[emptyTagIdx] = emptyTagIdx;
            }
        }


        int spanIdx = ROOT_INDEX;
        Span span = new Span(spanIdx, 0, null, null, words);
        span.setAnchor(0);
        spanIdx++;
        list.add(span);

        Tag beginTag = null, endTag = null;
        for (int t = 0; t < tags.length; t++) {
            Tag.Type type = tags[t].getType();
            if (tagLink[t] != -1) {
                if ((type == Tag.Type.OPENING_TAG) || (type == Tag.Type.EMPTY_TAG)) {
                    beginTag = tags[t];
                    endTag = tags[tagLink[t]];
                } else {
                    continue;
                }
            } else {
                if (type == Tag.Type.OPENING_TAG) {
                    beginTag = tags[t];
                    endTag = null;
                } else if (type == Tag.Type.CLOSING_TAG) {
                    beginTag = null;
                    endTag = tags[t];
                }
            }
            span = new Span(spanIdx, tagLevel[t], beginTag, endTag, words);
            list.add(span);
            spanIdx++;
        }
    }

    protected List<Span> asList() {
        return list;
    }

    protected void project(SpanCollection sourceSpans, Alignment alignment, int targetWords) {
        for (Span sourceSpan : sourceSpans) {
            Span targetSpan = new Span(sourceSpan.getId(), sourceSpan.getLevel(), sourceSpan.getBeginTag(), sourceSpan.getEndTag(), targetWords);
            if (sourceSpan.getId() == ROOT_INDEX) { //main span covering the full sentence
                //just set the anchor to 0
                targetSpan.setAnchor(0);
            } else {
                //compute and set new positions
                Coverage newPositions = new Coverage();
                for (int pos : sourceSpan.getPositions()) {
                    newPositions.addAll(alignment.get(pos));
                }
                newPositions.uniq();
                newPositions.sort();


                if (sourceSpan.getBeginTag() == null) {
                    //this should cover from the beginning of the sentence
                    if (newPositions.size() > 0) {
                        for (int i = newPositions.last() - 1; i >= 0; i--) {
                            newPositions.add(i);
                        }
                    }
                } else if (sourceSpan.getEndTag() == null) {
                    //this should cover till the end of the sentence
                    if (newPositions.size() > 0) {
                        for (int i = newPositions.last(); i < targetWords; i++) {
                            newPositions.add(i);
                        }
                    }
                }
                newPositions.uniq();
                newPositions.sort();

                targetSpan.clearPositions();
                targetSpan.addPositions(newPositions);

                //set new anchors if possible, otherwise invalidate them
                if (targetSpan.getPositions().size() > 0) {
                    targetSpan.setAnchor(targetSpan.getPositions().get(0));
                } else {
                    int sourceAnchor = sourceSpan.getAnchor();
                    if (alignment.get(sourceAnchor).size() > 0) {
                        targetSpan.setAnchor(alignment.get(sourceAnchor).first());
                    } else {
                        targetSpan.setAnchor(-1);
                    }
                }
            }
            this.add(targetSpan);
        }
    }

    private void add(Span s) {
        list.add(s);
    }

    protected int size() {
        return list.size();
    }

    protected Span get(int i) {
        return list.get(i);
    }

    @NotNull
    @Override
    public Iterator<Span> iterator() {
        return new Iterator<Span>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < list.size();
            }

            @Override
            public Span next() {
                Span pos = list.get(index);
                index++;
                return pos;
            }
        };
    }


    protected void print() {
        for (Span span : this.list) {
            System.out.println("Span " + span.getId() + " " + span.toString());
        }
    }

}
