package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.*;

import java.util.*;


public class TagProjector {

    private static int ROOT_LEVEL = 0;
    private static int ROOT_INDEX = 0;

/*
    private Tag[] htmlMapTags(Tag[] tags) {
        for (int t = 0 ; t < tags.length ; t++) {

        }
    }*/

    private Tag[] mapTags(Tag[] tags) {
        Tag[] mappedTags = new Tag[tags.length];
        for (int t = 0 ; t < tags.length ; t++){
            if (tags[t].getType() == Tag.Type.EMPTY_TAG) {
                if (tags[t].getName().equals("bx")) {
                    mappedTags[t] = Tag.fromText("<bx>",tags[t].hasLeftSpace(),tags[t].getRightSpace(),tags[t].getPosition());
                } else if (tags[t].getName().equals("ex")) {
                    mappedTags[t] = Tag.fromText("</ex>",tags[t].hasLeftSpace(),tags[t].getRightSpace(),tags[t].getPosition());
                }   else {
                    mappedTags[t] = tags[t];
                }
            } else {
                mappedTags[t] = tags[t];
            }
        }
        return mappedTags;
    }


    public Translation project(Translation translation) {
        //TODO: stub-implementation
        Sentence sentence = translation.getSource();

        Tag[] tags = sentence.getTags();
//        printTags(tags);


        Word[] sentenceWords = sentence.getWords();
        Word[] translationWords = translation.getWords();

        if (tags.length == 0) { //no tag to project; just return the translation
            //do nothing
        } else if (sentenceWords.length == 0) { //there are no source words; just copy the source tags in the target tags
            translation.setTags(tags);
        } else {
            Tag[] mappedTags = mapTags(tags);
//        printTags(mappedTags);
            Alignment alignment = translation.getWordAlignment();
            Tag[] translationTags = null;
            try {
                /*list of tags obtained by the tokenization process*/
                List<Span> sentenceSpans;
                Node<Span> sentenceRoot;
                sentenceSpans = createSpans(mappedTags, sentenceWords.length);
//            printSpans(sentenceSpans);

                sentenceRoot = createTree(sentenceSpans);

                sortTree(sentenceRoot);
//                printTree(sentenceRoot);


/*
                List<SortedSet<Integer>> dummyAlignmentList = getDummyAlignment(sentenceWords.length, translationWords.length);
                Node<Span> sentenceRootNew = projectTree(sentenceSpans, sentenceSpans, sentenceRoot, dummyAlignmentList, mappedTags);
                sortTree(sentenceRootNew);
*/
//                printTree(sentenceRootNew);

                List<SortedSet<Integer>> alignmentList = getAlignment(alignment, sentenceWords.length, translationWords.length);

                List<Span> translationSpans = projectSpan(sentenceSpans, alignmentList, translationWords.length);
//            printSpans(translationSpans);

                Node<Span> translationRoot = projectTree(sentenceSpans, translationSpans, sentenceRoot, alignmentList, mappedTags);

                sortTree(translationRoot);
//                printTree(translationRoot);

                translationTags = createTags(translationRoot);
//                printTags(translationTags);

            } catch (Exception e) {
                e.printStackTrace();
            }
            translation.setTags(translationTags);
        }
//        System.out.println("returning translation:" + translation);
        return translation;
    }

    private void sortTree(Node<Span> node) {
        node.sortChildren();

        for (Node<Span> child : node.getChildren()) {
            child.sortChildren();
        }
    }


    private List<SortedSet<Integer>> getDummyAlignment(int sourceWords, int targetWords) {
        //A Treeset is a sorted set
        List<SortedSet<Integer>> list = new ArrayList<>(sourceWords + 1);
        for (int i = 0 ; i < sourceWords + 1; i++) {
            list.add(new TreeSet<>());
            list.get(i).add(i);
        }

        return list;
    }
    private List<SortedSet<Integer>> getAlignment(Alignment alignment, int sourceWords, int targetWords) {
        //A Treeset is a sorted set
        List<SortedSet<Integer>> list = new ArrayList<>(sourceWords + 1);
        //create and empty Treeset for each source word; they may remain empty
        //an additional position is reserved for (sourceWords+1) which is used for tags anchored to the end of the sentence
        for (int i = 0 ; i < sourceWords + 1; i++) {
            list.add(new TreeSet<>());
        }
        for (int i = 0 ; i < alignment.getSourceIndexes().length; i++) {
            list.get(alignment.getSourceIndexes()[i]).add(alignment.getTargetIndexes()[i]);
        }
        //create an artificial alignment point between positions (sourceWords) and (targetWords) (first words after the sentence
        list.get(sourceWords).add(targetWords);

        return list;
    }

    private void printTags(Tag[] tags){
        for (int tagIdx = 0; tagIdx < tags.length; tagIdx++) {
            System.out.println("Tag " + tagIdx + " " + tags[tagIdx].toString() + " " + tags[tagIdx].getType() + " " + tags[tagIdx].getPosition());
        }
    }

    private void printSpans(List<Span> spans){
        for (int spanIdx = 0; spanIdx < spans.size(); spanIdx++) {
            System.out.println("Span " + spanIdx + " " + spans.get(spanIdx).toString());
        }
    }

    private void printTree(Node<Span> node) {
        printTree(node, "  ");
    }

    private void printTree(Node<Span> node, String appender) {
        System.out.println(appender + node.toString());
        node.getChildren().forEach(each ->  printTree(each, "  " + appender));
    }

    private List<Span> createSpans(Tag[] tags, int words) throws Exception {

        Map<String, List<Integer>> openingTagSet = new HashMap();
        Map<String, List<Integer>> closingTagSet = new HashMap();
        Map<String, List<Integer>> emptyTagSet = new HashMap();

        for (int tagIndex = 0; tagIndex < tags.length; tagIndex++) {
            if (tags[tagIndex].getType() ==  Tag.Type.OPENING_TAG) {
                if (openingTagSet.get(tags[tagIndex].getName()) == null) {
                    openingTagSet.put(tags[tagIndex].getName(), new ArrayList<>());
                }
                openingTagSet.get(tags[tagIndex].getName()).add(tagIndex);
            }
            if (tags[tagIndex].getType() ==  Tag.Type.CLOSING_TAG) {
                if (closingTagSet.get(tags[tagIndex].getName()) == null) {
                    closingTagSet.put(tags[tagIndex].getName(), new ArrayList<>());
                }
                closingTagSet.get(tags[tagIndex].getName()).add(tagIndex);
            }
            if (tags[tagIndex].getType() ==  Tag.Type.EMPTY_TAG) {
                if (emptyTagSet.get(tags[tagIndex].getName()) == null) {
                    emptyTagSet.put(tags[tagIndex].getName(), new ArrayList<>());
                }
                emptyTagSet.get(tags[tagIndex].getName()).add(tagIndex);
            }

        }

        int[] tagLevel = new int[tags.length];
        int[] tagLink = new int[tags.length];


        int level = ROOT_LEVEL;
        int minLevel = 0;
        for (int t = 0; t < tags.length; t++) {
            if (tags[t].getType() == Tag.Type.EMPTY_TAG) {
                //do nothing
                tagLevel[t] = level;
                minLevel = minLevel < level ? minLevel : level;
            } else if (tags[t].getType() == Tag.Type.OPENING_TAG) {
                tagLevel[t] = level;
                level++;
                minLevel = minLevel < level ? minLevel : level;
            } else if (tags[t].getType() == Tag.Type.CLOSING_TAG) {
                level--;
                tagLevel[t] = level;
                minLevel = minLevel < level ? minLevel : level;
            }
        }
        minLevel--;
        for (int t = 0; t < tagLevel.length; t++) {
            tagLevel[t] = tagLevel[t] - minLevel;
        }

        boolean[] tagVisit = new boolean[tags.length];

        List<Span> spans = new ArrayList<>();
        Tag beginTag = null;
        Tag endTag = null;


        for (String name : openingTagSet.keySet()) {

            List<Integer> openingTags = (ArrayList<Integer>) openingTagSet.get(name);
            List<Integer> closingTags = (ArrayList<Integer>) closingTagSet.get(name);
            if (closingTags == null) { // there are no closing tags for this name; hence all opening tags for this name are spurious

                for (int bt = openingTags.size() - 1; bt >= 0; bt--) {
                    int beginTagIdx = openingTags.get(bt);
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
                    for (int et = 0; et < closingTags.size(); et++) {
                        int idx = closingTags.get(et);
                        if (!tagVisit[idx] && idx > beginTagIdx) {
                            endTagIdx = idx;
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
                        beginTag = tags[beginTagIdx];
                        endTag = null;
                        tagVisit[beginTagIdx] = true;
                        tagLink[beginTagIdx] = -1;

                    }
                }
            }
        }

        for (String name : closingTagSet.keySet()) {

            List<Integer> openingTags = (ArrayList<Integer>) openingTagSet.get(name);
            List<Integer> closingTags = (ArrayList<Integer>) closingTagSet.get(name);
            if (openingTags == null) { // there are no opening tags for this name; hence all closing tags for this name are spurious

                for (int et = closingTags.size() - 1; et >= 0; et--) {
                    int endTagIdx = closingTags.get(et);
                    tagVisit[endTagIdx] = true;
                    tagLink[endTagIdx] = -1;
                }
            } else {
                for (int et = 0; et < closingTags.size(); et++) {

                    int endTagIdx = closingTags.get(et);

                    if (tagVisit[endTagIdx]) {
                        continue;
                    }

                    int beginTagIdx = -1;
                    for (int bt = openingTags.size() - 1; bt >= 0; bt--) {
                        int idx = openingTags.get(bt);
                        if (!tagVisit[idx] && idx > endTagIdx) {
                            beginTagIdx = idx;
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
                        //found opening tag without closing
                        //create the corresponding span opening span without closing
                        //visit opening tag
                        endTag = tags[endTagIdx];
                        tagLink[endTagIdx] = -1;

                    }
                }
            }
        }

        for (String name : emptyTagSet.keySet()) {
            List<Integer> emptyTags = (ArrayList<Integer>) emptyTagSet.get(name);
            for (int t = 0; t < emptyTags.size(); t++) {

                int tagIdx = emptyTags.get(t);
                tagLink[tagIdx] = -1;
            }
        }

        int spanIdx = ROOT_INDEX;
        Span span = new Span(spanIdx, level, beginTag, endTag, words);
        spanIdx++;
        spans.add(span);
        for (int t = 0; t < tags.length; t++) {
            if (tagLink[t] != -1) {
                if (tags[t].getType() == Tag.Type.OPENING_TAG) {
                    beginTag = tags[t];
                    endTag = tags[tagLink[t]];
                } else {
                    continue;
                }
            } else {
                if (tags[t].getType() == Tag.Type.OPENING_TAG) {
                    beginTag = tags[t];
                    endTag = null;
                } else if (tags[t].getType() == Tag.Type.CLOSING_TAG) {
                    beginTag = null;
                    endTag = tags[t];
                } else {
                    beginTag = tags[t];
                    endTag = tags[t];
                }
            }
            span = new Span(spanIdx, tagLevel[t], beginTag, endTag, words);
            spans.add(span);
            spanIdx++;
        }
//        printSpans(spans);

        return spans;

        /*
//        {
//        List<Span> spans = new ArrayList<>();

//        int level = ROOT_LEVEL;
//        int spanIdx = ROOT_INDEX;

            // insert the root span, which does not have any opening or closing tags, and which covers all positions
//        Span span = new Span(spanIdx, level, null, null, words);
            spanIdx++;
            spans.add(span);

//        boolean[] tagVisit = new boolean[tags.length];
            for (int beginTagIndex = 0; beginTagIndex < tags.length; beginTagIndex++) {
                System.out.println("HERE 1 beginTagIndex:" + beginTagIndex + " tagVisit:" + tagVisit[beginTagIndex] + " type:" + tags[beginTagIndex].getType());
                if (tagVisit[beginTagIndex]) {
                    if (tags[beginTagIndex].getType() == Tag.Type.CLOSING_TAG) {
                        level--;
                    }
                    continue;
                }

                System.out.println("HERE 2 beginTagIndex:" + beginTagIndex + " tagVisit:" + tagVisit[beginTagIndex] + " type:" + tags[beginTagIndex].getType());

                if (tags[beginTagIndex].getType() == Tag.Type.EMPTY_TAG) {
                    span = new Span(spanIdx, level + 1, tags[beginTagIndex], tags[beginTagIndex], words);
                    spanIdx++;
                    spans.add(span);
                    tagVisit[beginTagIndex] = true;
                } else if (tags[beginTagIndex].getType() == Tag.Type.OPENING_TAG) {
                    level++;
                    int endTagIndex = tags.length - 1;
                    while (endTagIndex > beginTagIndex) {

                        if (tagVisit[endTagIndex]) {
                            endTagIndex--;
                            continue;
                        }
                        if (tags[endTagIndex].getType() == Tag.Type.CLOSING_TAG && tags[endTagIndex].getName().equals(tags[beginTagIndex].getName())) {

                            span = new Span(spanIdx, level, tags[beginTagIndex], tags[endTagIndex], words);
                            spanIdx++;
                            spans.add(span);
                            tagVisit[beginTagIndex] = true;
                            tagVisit[endTagIndex] = true;
                            break;
                        }
                        endTagIndex--;
                    }
                } else if (tags[beginTagIndex].getType() == Tag.Type.CLOSING_TAG) {
                    //this is a spurious tag (closing of nothing),
                    //this is case is
                }
            }

            // Fix spurious tags
            int firstStillOpened = Integer.MAX_VALUE;
            int openedAtRight = 0;
            int tagIdx = 0;
            for (int t = 0; t < tagVisit.length; t++) {
                System.out.println("t:" + t + " tagVisit:" + tagVisit[t]);
            }
            while (firstStillOpened == Integer.MAX_VALUE && tagIdx < tags.length) {
                System.out.println("tagIdx:" + tagIdx + " firstStillOpened:" + firstStillOpened + " tagVisit[tagIdx]:" + tagVisit[tagIdx] + " tags[tagIdx].getType()" + tags[tagIdx].getType());
                if (tagVisit[tagIdx] == false && tags[tagIdx].getType() == Tag.Type.OPENING_TAG) {
                    firstStillOpened = tagIdx;
                }
                tagIdx++;
            }
            tagIdx = (firstStillOpened == Integer.MAX_VALUE) ? Integer.MAX_VALUE : Math.min(Integer.MAX_VALUE, firstStillOpened + 1);

            System.out.println("tagIdx:" + tagIdx + " tagVisit.length" + tagVisit.length);
            while (tagIdx < tagVisit.length) {
                if (tagVisit[tagIdx] == true) {
                    System.out.println("tagIdx:" + tagIdx + " tagVisit[tagIdx]:" + tagVisit[tagIdx] + " tags[tagIdx].getType()" + tags[tagIdx].getType());
                    System.out.println("openedAtRight:" + openedAtRight);
                    if (tags[tagIdx].getType() == Tag.Type.OPENING_TAG) {
                        openedAtRight++;
                    } else if (tags[tagIdx].getType() == Tag.Type.CLOSING_TAG) {
                        openedAtRight--;
                    }
                }
                tagIdx++;
            }
            if (openedAtRight != 0) {
                throw new Exception("tags are not xml compliant");
            }

            // fix opening tags without closing tags
            level = 0;
            for (tagIdx = firstStillOpened; tagIdx < tagVisit.length; tagIdx++) {
                if (tagVisit[tagIdx] == false) {
                    if (tags[tagIdx].getType() == Tag.Type.OPENING_TAG) {
                        level++;
                        //inserting span covering from tagIdx till the end of the sentence
                        span = new Span(spanIdx, level, tags[tagIdx], null, words);
                        spanIdx++;
                        spans.add(span);
                        tagVisit[tagIdx] = true;
                    }
                }
            }

            int lastStillClosed = Integer.MIN_VALUE;
            int closedAtLeft = 0;
            tagIdx = tagVisit.length - 1;
            while (lastStillClosed == Integer.MIN_VALUE && tagIdx >= 0) {
                if (tagVisit[tagIdx] == false && tags[tagIdx].getType() == Tag.Type.CLOSING_TAG) {
                    lastStillClosed = tagIdx;
                }
                tagIdx--;
            }
            tagIdx = (lastStillClosed == Integer.MIN_VALUE) ? Integer.MIN_VALUE : Math.max(Integer.MIN_VALUE, lastStillClosed - 1);

            while (tagIdx >= 0) {
                if (tagVisit[tagIdx] == true) {
                    if (tags[tagIdx].getType() == Tag.Type.OPENING_TAG) {
                        closedAtLeft++;
                    } else if (tags[tagIdx].getType() == Tag.Type.CLOSING_TAG) {
                        closedAtLeft--;
                    }
                }
                tagIdx--;
            }
            if (closedAtLeft != 0) {
                throw new Exception("tags are not xml compliant");
            }


            // fix opening tags without closing tags
            level = 0;
            for (tagIdx = lastStillClosed; tagIdx >= 0; tagIdx--) {
                if (tagVisit[tagIdx] == false) {
                    level++;
                    //inserting span covering from the beginning of the sentence till tagIdx
                    span = new Span(spanIdx, level, null, tags[tagIdx], words);
                    spanIdx++;
                    spans.add(span);
                    tagVisit[tagIdx] = true;
                }
            }

            return spans;
        }
        */

    }

    private Node<Span> createTree(List<Span> spans) throws Exception {
        List<Boolean> spanVisit = new ArrayList<>(spans.size());
        for (Span span: spans) {
            spanVisit.add(false);
        }
        return createTree(spans, ROOT_INDEX, spanVisit);
    }

    private Node<Span> createTree(List<Span> spans, int spanIdx, List<Boolean> spanVisit) throws Exception {
        Node<Span> root = new Node<>(spans.get(spanIdx));
        int rootLevel = root.getData().getLevel();

        spanVisit.set(spanIdx, true);

        int firstChildIdx = spanIdx + 1;
        //search for the first span on the right which has level = root.getLevel()+1, but is not child of a sibling
        while (firstChildIdx < spans.size()) {
            if (spans.get(firstChildIdx).getLevel() == rootLevel) {
                break;
            }
            if (spans.get(firstChildIdx).getLevel() == rootLevel + 1) {
                break;
            }
            firstChildIdx++;
        }

        // consider all spans from firstChildIdx to the last span
        int idx = firstChildIdx;
        while (idx < spans.size()) {
            if (!spanVisit.get(idx)) {
                Span span = spans.get(idx);
                assert (span.getLevel() >= root.getData().getLevel());
                if (span.getLevel() <= root.getData().getLevel()) {
                    break;
                }

                Node<Span> node = createTree(spans, idx, spanVisit);
                if (span.getLevel() == rootLevel + 1) {
                    root.addChild(node);
                }
            }
            idx++;

        }
//        System.out.println("tree when condiering root:" + root);
//        printTree(root);

        firstChildIdx = spanIdx - 1;
        //search for the first span on the right having level = root.getLevel()+1
        while (firstChildIdx >= 0 && (spans.get(firstChildIdx).getLevel() != rootLevel + 1)) {
            if (spans.get(firstChildIdx).getLevel() == rootLevel) {
                break;
            }
            if (spans.get(firstChildIdx).getLevel() == rootLevel + 1) {
                break;
            }
            firstChildIdx--;
        }

        // consider all spans on the right of the root
        idx = firstChildIdx;
        while (idx >= 0) {

            if (!spanVisit.get(idx)) {
                Span span = spans.get(idx);
                assert (span.getLevel() >= root.getData().getLevel());
                if (span.getLevel() <= root.getData().getLevel()) {
                    break;
                }

                Node<Span> node = createTree(spans, idx, spanVisit);
                if (span.getLevel() == rootLevel + 1) {
                    root.addChild(node);
                }
            }
            idx--;
        }
        return root;
    }

    private List<Span> projectSpan(List<Span> sourceSpans, List<SortedSet<Integer>> alignmentList, int targetWords) {

        List<Span> targetSpans = new ArrayList<>();

        for (Span sourceSpan : sourceSpans) {
            Span targetSpan = new Span(sourceSpan.getId(), sourceSpan.getLevel(), sourceSpan.getBeginTag(), sourceSpan.getEndTag(), targetWords);
            if (sourceSpan.getId() == ROOT_INDEX) { //main span covering the full sentence
                //do nothing
            } else {
                //compute and set new positions
                SortedSet<Integer> newPositions = new TreeSet<>();
                for (int pos : sourceSpan.getPositions()) {
                    newPositions.addAll(alignmentList.get(pos));
                }

                if (sourceSpan.getBeginTag() == null) {
                    //this should cover from the beginning of the sentence
                    if (newPositions.size() > 0) {
                        for (int i = 0; i <= newPositions.last(); i++) {
                            newPositions.add(i);
                        }
                    }
                } else if (sourceSpan.getEndTag() == null) {
                    //this should cover till the end of the sentence
                    if (newPositions.size() > 0) {
                        for (int i = newPositions.first(); i < targetWords; i++) {
                            newPositions.add(i);
                        }
                    }
                }
                targetSpan.clearPositions();
                targetSpan.addPositions(newPositions);
                //invalidate new anchor
                targetSpan.setAnchor(-1);
            }
            targetSpans.add(targetSpan);
        }
        return targetSpans;
    }

    private Node<Span> projectTree(List<Span> sourceSpans, List<Span> targetSpans, Node<Span> sourceRoot, List<SortedSet<Integer>> alignmentList, Tag[] tags) {
        Node<Span> targetRoot = cloneTree(sourceSpans, targetSpans, sourceRoot);

        Set<Node<Span>> nodeVisit = new HashSet<>();
        fixNode(targetRoot, nodeVisit);
//        printTree(sourceRoot);
        fixAnchors(targetRoot, sourceRoot, alignmentList);
        fixUndefinedAnchors(targetRoot);
//        printTree(targetRoot);
        return targetRoot;
    }

    private void fixUndefinedAnchors(Node<Span> node) {

        if (node.getChildren().size() >= 2) {
            for (int i = 0; i < node.getChildren().size(); i++) {
                Node<Span> childI = node.getChildren().get(i);

                int anchorI = childI.getData().getAnchor();
                if (anchorI == -1 && childI.getData().getBeginTag() == null) {

                    for (int j = i+1; j < node.getChildren().size(); j++) {
                        Node<Span> childJ = node.getChildren().get(j);
                        int newAnchorI = childJ.getData().getAnchor();
                        if (newAnchorI != -1) {
                            anchorI = newAnchorI;
                            break;
                        }
                    }
                    childI.getData().setAnchor(anchorI);
                }
            }


            for (int i = node.getChildren().size() - 1; i >= 0; i--) {
                Node<Span> childI = node.getChildren().get(i);

                int anchorI = childI.getData().getAnchor();
                if (anchorI == -1 && childI.getData().getEndTag() == null) {

                    for (int j = i-1; j >= 0; j--) {
                        Node<Span> childJ = node.getChildren().get(j);
                        int newAnchorI = childJ.getData().getAnchor();
                        int s = childJ.getData().getPositions().size();
                        if (s > 0) {
                            newAnchorI = childJ.getData().getPositions().get(s-1) + 1;
                        }
                        if (newAnchorI != -1) {
                            anchorI = newAnchorI;
                            break;
                        }
                    }
                    childI.getData().setAnchor(anchorI);
                }
            }

        }
    }

    private void fixAnchors(Node<Span> node, Node<Span> sourceRoot, List<SortedSet<Integer>> alignmentList) {
        int anchor = 0;
        Span targetSpan = node.getData();
        int spanIdx = targetSpan.getId();
        Node<Span> sourceNode = sourceRoot.getNode(spanIdx); // get the corresponding sourceNode
        anchor = closestAnchor(node, sourceNode, alignmentList);
        node.getData().setAnchor(anchor);
        for (Node<Span> child : node.getChildren()) {
            fixAnchors(child, sourceRoot, alignmentList);
        }
    }

    private int closestAnchor(Node<Span> node, Node<Span> sourceNode, List<SortedSet<Integer>> alignmentList) {
        int targetAnchor ;

        if (sourceNode.getData().getBeginTag() == null) {
            targetAnchor = -1;
        } else if (sourceNode.getData().getEndTag() == null) {
            if (alignmentList.get(sourceNode.getData().getAnchor()).size() > 0) {
                targetAnchor = alignmentList.get(sourceNode.getData().getAnchor()).last();
            } else {
                targetAnchor = -1;
            }
        } else {
            ArrayList<Integer> contiguousPositions = contiguous(node.getData().getPositions());
            ArrayList<Integer> positions = new ArrayList<>();
            positions.add(sourceNode.getData().getAnchor());
            positions.addAll(sourceNode.getData().getPositions());
            targetAnchor = -1;
            for (int closest : positions) {
                if (alignmentList.get(closest).size() > 0) {
                    targetAnchor = alignmentList.get(closest).first();
                    if (contiguousPositions.contains(targetAnchor)) {
                        break;
                    } else {
                        if (node.getParent() == null) {
                            targetAnchor = closestAnchor(node.getParent(), sourceNode, alignmentList);
                        }
                        if (targetAnchor != -1) {
                            break;
                        }
                    }
                }
            }
        }
        return targetAnchor;
    }

    private void  fixNode(Node<Span> node, Set<Node<Span>> nodeVisit) {
        if (nodeVisit.contains(node)) {
        } else if (node.getChildren().size() == 0) { // there are no children
            //do nothing; just label as visited
            nodeVisit.add(node);
        } else if (node.getChildren().size() == 1) { // there is only one child
            nodeVisit.add(node);
            Iterator<Node<Span>> iteratorI = node.getChildren().iterator();
            while (iteratorI.hasNext()) {
                Node<Span> childI = iteratorI.next();
                fixNode(childI, nodeVisit);
            }
        } else { // there are at least two children

            Iterator<Node<Span>> iteratorI = node.getChildren().iterator();
            Node<Span> childI = null;
            ArrayList<Integer> positionsToRemove = new ArrayList<>();

            boolean modifiedI = true;
            boolean modifiedJ = true;
            while (iteratorI.hasNext()) {
                modifiedI = false;
                modifiedJ = false;
                childI = iteratorI.next();
                Node<Span> childJ = null;

                if (nodeVisit.contains(childI)) {
                    continue;   //childI and its descendant are already fixed
                }

                Iterator<Node<Span>> iteratorJ = node.getChildren().iterator();
                while (!modifiedI && !modifiedJ && iteratorJ.hasNext()) {
                    childJ = iteratorJ.next();

                    if (childI != childJ) {
                        ArrayList<Integer> posI = childI.getData().getPositions();
                        ArrayList<Integer> posJ = childJ.getData().getPositions();

                        ArrayList<Integer> contiguousIntersection = intersection(contiguous(posI), contiguous(posJ));
                        if (contiguousIntersection.size() != 0) {
                            // choose one point to exclude from both childI and childJ
                            // so that their intersection is minimal

                            positionsToRemove = choosePositions(posI, posJ);

                            // remove positions and flag if the removal occurs
                            modifiedI = false;
                            modifiedJ = false;
                            Iterator<Integer> iterator = positionsToRemove.iterator();
                            while (!modifiedI && !modifiedJ && iterator.hasNext()) {
                                Integer pos = iterator.next();
//                            for (Integer pos : positionsToRemove) {
                                modifiedI = modifiedI || posI.remove(pos);
                                if (!modifiedI) {
                                    modifiedJ = modifiedJ || posJ.remove(pos);
                                }
                            }
                        } else {
                            //do nothing; the two children are not overlapping
                        }
                    }
                }
                if (modifiedI) {
                    // recursive remove positionsToRemove from childI
                    fixChildren(childI, positionsToRemove);
                } else if (modifiedJ) {
                    // recursive remove positionsToRemove from childJ
                    fixChildren(childJ, positionsToRemove);
                }

                if (modifiedI || modifiedJ) {
                    break;
                }
            }

            if (modifiedI || modifiedJ) {
                fixNode(node, nodeVisit);
            } else {
                nodeVisit.add(node);
                iteratorI = node.getChildren().iterator();
                while (iteratorI.hasNext()) {
                    childI = iteratorI.next();
                    fixNode(childI, nodeVisit);
                }
            }
        }


    }

    private void fixChildren(Node<Span> node, ArrayList<Integer> positionsToRemove) {
        ArrayList<Integer> nodePositions = node.getData().getPositions();

        List<Node<Span>> childrenToRemove = new ArrayList<>();
        for (Node<Span> child : node.getChildren()) {
            ArrayList<Integer> childPositions = node.getData().getPositions();

            if ( childPositions.size() > 0 ) {
                ArrayList<Integer> intersection = intersection(childPositions, nodePositions);
                if (intersection.size() == childPositions.size()) {
                    // child is not totally contained in  node

                    if (intersection.size() > 0) {
                        // child is only partially contained in  node
                        for (Integer pos : positionsToRemove) {
                            // remove the positions from child already removed from node
                            child.getData().getPositions().remove(pos);
                        }
                    } else {
                        // child becomes a sibling of node
                        child.getData().setLevel(node.getData().getLevel());
                        childrenToRemove.add(child);
                    }
                    fixChildren(child, positionsToRemove);
                }
            }
        }
        for (Node<Span> child : childrenToRemove) {
            node.removeChild(child);
            node.getParent().addChild(child);
        }
    }

    private Node<Span> cloneTree(List<Span> sourceSpans, List<Span> targetSpans, Node<Span> sourceNode) {
        Node<Span> cloneNode = new Node<>(targetSpans.get(sourceNode.getData().getId()));
        for (Node<Span> sourceChild : sourceNode.getChildren()){
            Node<Span> cloneChild = cloneTree(sourceSpans, targetSpans, sourceChild);
            cloneNode.addChild(cloneChild);
        }
        return cloneNode;
    }

    static public ArrayList<Integer> intersection(ArrayList<Integer> posI, ArrayList<Integer> posJ) {
        ArrayList<Integer> intersection = new ArrayList<>();
        for (Integer pos : posI) {
            if (posJ.contains(pos)) {
                intersection.add(pos);
            }
        }
        return intersection;
    }

    static public boolean included(ArrayList<Integer> posI, ArrayList<Integer> posJ) {
        // return true if posI is included in posJ
        for (Integer pos : posI) {
            if (posJ.contains(pos)) {
                return false;
            }
        }
        return true;
    }


    static private ArrayList<Integer> contiguous(ArrayList<Integer> positions) {
        if (positions.size() >0) {
            ArrayList<Integer> contiguous = new ArrayList(positions.size());
            for (int i = Collections.min(positions); i <= Collections.max(positions); i++) {
                contiguous.add(i);
            }
            return contiguous;
        } else {
            return new ArrayList(positions.size());
        }
    }

    static public int choosePosition(ArrayList<Integer> posI, ArrayList<Integer> posJ) {
        int minI = Collections.min(posI);
        int minJ = Collections.min(posJ);
        int maxJ = Collections.max(posJ);
        if (minI <= minJ) {
            return maxJ;
        } else {
            return minJ;
        }
    }

    static public ArrayList<Integer> choosePositions(final ArrayList<Integer> posI, final ArrayList<Integer> posJ) {
        ArrayList<Integer> positions = new ArrayList<>();

        ArrayList<Integer> tmpPosI = new ArrayList<>(posI);
        ArrayList<Integer> tmpPosJ = new ArrayList<>(posJ);
        while (true) {
            Integer chosenP = choosePosition(tmpPosI, tmpPosJ);

            tmpPosI.remove(chosenP);
            tmpPosJ.remove(chosenP);

            positions.add(chosenP);

            ArrayList<Integer> tmpIntersection = intersection(contiguous(tmpPosI), contiguous(tmpPosJ));
            if (tmpIntersection.size() == 0) {
                break;
            }
        }
        return positions;
    }

    private Tag[] createTags(Node<Span> node) {
        ArrayList<Tag> tags = new ArrayList<>();

        createTags(node, tags);

        Tag[] t = new Tag[tags.size()];
        return tags.toArray(t);
    }

    private void createTags(Node<Span> node, ArrayList<Tag> tags) {
        Span span = node.getData();

        if (span.getBeginTag() != null) {
            Tag t = span.getBeginTag();
            if (span.getAnchor() == -1) {
                t.setPosition(0);
            } else {
                t.setPosition(span.getAnchor());
            }
            tags.add(t);
        }
        for (Node<Span> child : node.getChildren()) {
            createTags(child, tags);
        }
        if (span.getEndTag() != null && span.getBeginTag() != span.getEndTag() ) {
            Tag t = span.getEndTag();
            if (span.getPositions().size() > 0) {
                t.setPosition(Collections.max(span.getPositions()) + 1);
            } else {
                if (span.getAnchor() == -1) {
                    t.setPosition(0);
                } else {
                    t.setPosition(span.getAnchor());
                }
            }
            tags.add(t);
        }
    }

}