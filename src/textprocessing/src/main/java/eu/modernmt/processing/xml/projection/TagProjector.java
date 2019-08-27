package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.*;

import java.util.*;


public class TagProjector {

    private static int ROOT_LEVEL = 0;
    private static int ROOT_INDEX = 0;

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
        Sentence sentence = translation.getSource();

        Tag[] tags = sentence.getTags();
        Word[] sentenceWords = sentence.getWords();
        Word[] translationWords = translation.getWords();

        if (tags.length != 0) {
            if (sentenceWords.length == 0) { //there are no source words; just copy the source tags in the target tags
                translation.setTags(tags);
            } else {
                InputFormatMap mapper = InputFormatMapFactory.build(tags);
                Tag[] mappedTags = mapper.transform(tags);
                Alignment alignment = translation.getWordAlignment();

                Tag[] translationTags = null;
                try {
                    /*list of tags obtained by the tokenization process*/
                    List<Span> sentenceSpans = createSpans(mappedTags, sentenceWords.length);
                    Node<Span> sentenceRoot = createTree(sentenceSpans);
                    sortTree(sentenceRoot);

                    List<SortedSet<Integer>> alignmentList = getAlignment(alignment, sentenceWords.length, translationWords.length);

                    List<Span> translationSpans = projectSpan(sentenceSpans, alignmentList, translationWords.length);
                    Node<Span> translationRoot = projectTree(translationSpans, sentenceRoot, alignmentList, translationWords.length);
                    sortTree(translationRoot);

                    translationTags = createTags(translationRoot);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                translation.setTags(translationTags);
            }
        } else { //no tag to project; just return the translation
            //do nothing
        }
        return translation;
    }

    private void sortTree(Node<Span> node) {
        node.sortChildren();

        for (Node<Span> child : node.getChildren()) {
            child.sortChildren();
        }
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

    private List<Span> createSpans(Tag[] tags, int words) {

        Map<String, List<Integer>> openingTagSet = new HashMap<>();
        Map<String, List<Integer>> closingTagSet = new HashMap<>();
        Map<String, List<Integer>> emptyTagSet = new HashMap<>();

        for (int tagIndex = 0; tagIndex < tags.length; tagIndex++) {
            Tag tag = tags[tagIndex];
            String name = tag.getName();
            if (tag.getType() ==  Tag.Type.OPENING_TAG) {
                openingTagSet.computeIfAbsent(name, k -> new ArrayList<>());
                openingTagSet.get(name).add(tagIndex);
            }
            if (tag.getType() ==  Tag.Type.CLOSING_TAG) {
                closingTagSet.computeIfAbsent(name, k -> new ArrayList<>());
                closingTagSet.get(tag.getName()).add(tagIndex);
            }
            if (tag.getType() ==  Tag.Type.EMPTY_TAG) {
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
                    for (int et = 0; et < closingTags.size(); et++) {
                        int idx = closingTags.get(et);
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

        List<Span> spans = new ArrayList<>();
        Tag beginTag = null, endTag = null;

        int spanIdx = ROOT_INDEX;
        Span span = new Span(spanIdx, level, beginTag, endTag, words);
        span.setAnchor(0);
        spanIdx++;
        spans.add(span);

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
                } else {
                    //do nothing
                }
            }
            span = new Span(spanIdx, tagLevel[t], beginTag, endTag, words);
            spans.add(span);
            spanIdx++;
        }

        return spans;
    }

    private Node<Span> createTree(List<Span> spans) {
        List<Boolean> spanVisit = new ArrayList<>(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            spanVisit.add(false);
        }
        return createTree(spans, ROOT_INDEX, spanVisit);
    }

    private Node<Span> createTree(List<Span> spans, int spanIdx, List<Boolean> spanVisit) {
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
                //just set the anchor to 0
                targetSpan.setAnchor(0);
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

                //set new anchors if possible, otherwise invalidate them
                if (targetSpan.getPositions().size() > 0) {
                    targetSpan.setAnchor(targetSpan.getPositions().get(0));
                } else {
                    int sourceAnchor = sourceSpan.getAnchor();
                    if (alignmentList.get(sourceAnchor).size() > 0) {
                        targetSpan.setAnchor(alignmentList.get(sourceAnchor).first());
                    } else {
                        targetSpan.setAnchor(-1);
                    }
                }
            }
            targetSpans.add(targetSpan);
        }
        return targetSpans;
    }

    private Node<Span> projectTree(List<Span> targetSpans, Node<Span> sourceRoot, List<SortedSet<Integer>> alignmentList,int targetWords) {
        Node<Span> targetRoot = cloneTree(targetSpans, sourceRoot);

        Set<Node<Span>> nodeVisit = new HashSet<>();
        fixNode(targetRoot, nodeVisit);

        fixAnchors(targetRoot, alignmentList, targetWords);

        fixUndefinedAnchors(targetRoot);

        return targetRoot;
    }

    private void fixUndefinedAnchors(Node<Span> node) {
//        System.out.println("fixUndefinedAnchors on node:" + node);
        if (node.getChildren().size() >= 2) {
            //fix anchor of children having beginTag == null; it assign the anchor of the closest right span with anchors ~= -1
            for (int i = 0; i < node.getChildren().size(); i++) {
                Node<Span> childI = node.getChildren().get(i);

                int newAnchor = childI.getData().getAnchor();
                if (newAnchor == -1 && childI.getData().getBeginTag() == null) {

                    for (int j = i + 1; j < node.getChildren().size(); j++) {
                        Node<Span> childJ = node.getChildren().get(j);
                        newAnchor = childJ.getData().getAnchor();
                        if (newAnchor != -1) {
                            break;
                        }
                    }
                    childI.getData().setAnchor(newAnchor);
                }
            }

            //fix anchor of children having endTag == null; it assign the anchor of the closest left span with anchors ~= -1
            for (int i = node.getChildren().size() - 1; i >= 0; i--) {
                Node<Span> childI = node.getChildren().get(i);

                int newAnchor = childI.getData().getAnchor();
                if (newAnchor == -1 && childI.getData().getEndTag() == null) {

                    for (int j = i - 1; j >= 0; j--) {
                        Node<Span> childJ = node.getChildren().get(j);
                        newAnchor = childJ.getData().getAnchor();
                        if (newAnchor != -1) {
                            break;
                        }
                        int sz = childJ.getData().getPositions().size();
                        newAnchor = (sz > 0) ? childJ.getData().getPositions().get(sz - 1) + 1 : newAnchor;

                        if (newAnchor != -1) {
                            break;
                        }
                    }
                    childI.getData().setAnchor(newAnchor);
                }
            }
            //fix anchor of children having both beginTag end endTag != null; it assign the anchor of the closest left or right span with anchors ~= -1
            for (int i = node.getChildren().size() - 1; i >= 0; i--) {
                Node<Span> childI = node.getChildren().get(i);

                int anchorI = childI.getData().getAnchor();
                if (anchorI == -1 && childI.getData().getEndTag() != null && childI.getData().getBeginTag() != null) {

                    int leftClosest = 0;
                    int leftAnchor = -1;
                    for (int j = i - 1; j >= 0; j--) {
                        Node<Span> childJ = node.getChildren().get(j);
                        leftAnchor = childJ.getData().getAnchor();
                        if (leftAnchor != -1) {
                            leftClosest = j;
                            break;
                        }
                        int sz = childJ.getData().getPositions().size();
                        leftAnchor = (sz > 0) ? childJ.getData().getPositions().get(sz - 1) + 1 : leftAnchor;
                        if (leftAnchor != -1) {
                            leftClosest = j;
                            break;
                        }
                    }

                    int rightClosest = node.getChildren().size();
                    int rightAnchor = -1;
                    for (int j = i + 1; j < node.getChildren().size(); j++) {
                        Node<Span> childJ = node.getChildren().get(j);
                        rightAnchor = childJ.getData().getAnchor();
                        if (rightAnchor != -1) {
                            rightClosest = j;
                            break;
                        }
                    }
                    int newAnchor;
                    if (rightAnchor != -1) {
                        if (leftAnchor != -1) {
                            newAnchor = (i - leftClosest < i - rightClosest) ? leftAnchor : rightAnchor;
                        } else {
                            newAnchor = rightAnchor;
                        }
                    } else {
                        newAnchor = leftAnchor;

                    }

                    childI.getData().setAnchor(newAnchor);
                }
            }
        } else {
            if ((node.getData().getAnchor() == -1) && (node.getParent() != null)) {
                node.getData().setAnchor(node.getParent().getData().getAnchor());
            }
        }

        for (Node<Span> child : node.getChildren()) {
            fixUndefinedAnchors(child);
        }
    }

    private void fixAnchors(Node<Span> node, List<SortedSet<Integer>> alignmentList, int targetWords) {
        Span targetSpan = node.getData();
        if (targetSpan.getAnchor() == -1) {
            targetSpan.setAnchor(computeAnchor(node, alignmentList, targetWords));
        }
        //overwrite anchors of empty tags
        if ((targetSpan.getBeginTag() != null) && (targetSpan.getBeginTag().getType() == Tag.Type.EMPTY_TAG)) {
            targetSpan.setAnchor(computeAnchorForSelfClosing(targetSpan.getBeginTag().getPosition(), alignmentList, targetWords));
        }
        for (Node<Span> child : node.getChildren()) {
            fixAnchors(child, alignmentList, targetWords);
        }
    }

    private int computeAnchor(Node<Span> node, List<SortedSet<Integer>> alignmentList,int targetWords) {
        int targetAnchor = -1;
        Span span = node.getData();
        if (span.getBeginTag() == null) {
            targetAnchor = 0;
        } else {
            if (span.getPositions().size() > 0) { //node with at least 1 contained token
                targetAnchor = span.getPositions().get(0);
            } else if (span.getBeginTag().getType() == Tag.Type.EMPTY_TAG) {
                targetAnchor = computeAnchorForSelfClosing(span.getBeginTag().getPosition(), alignmentList, targetWords);
            }
        }

        return targetAnchor;
    }

    private int computeAnchorForSelfClosing(int sourcePosition, List<SortedSet<Integer>> alignmentList,int targetWords) {
        Set<Integer> sourceLeftToken = new HashSet<>();
        Set<Integer> sourceRightToken = new HashSet<>();
        Set<Integer> targetLeftToken = new HashSet<>();
        Set<Integer> targetRightToken = new HashSet<>();
        Set<Integer> leftTokenIntersection = new HashSet<>();
        Set<Integer> rightTokenIntersection = new HashSet<>();

        //Words that are at the left of the tag in the source sentence, should be at left of the mapped tag
        //in the translation. Some reasoning for those that are at the right.

        for (int sourceP = 0; sourceP < alignmentList.size(); sourceP++) {
            if (sourceP < sourcePosition) {
                //If the word is at the left of the current tag
                //Remember that it should be at the left also in the translation
                sourceLeftToken.addAll(alignmentList.get(sourceP));
            } else {
                //Remember that it should be at the right also in the translation
                sourceRightToken.addAll(alignmentList.get(sourceP));
            }
        }

        //Find the mapped position that respects most of the left-right word-tag relationship as possible.
        for (int i = 0; i < targetWords; i++) {
            targetRightToken.add(i);
        }
        rightTokenIntersection.addAll(sourceRightToken);
        rightTokenIntersection.retainAll(targetRightToken);
        int maxScore = rightTokenIntersection.size();
        int bestPosition = 0;
        int actualPosition = 0;
        for (int i = 0; i < targetWords; i++) {
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
            if (score >= maxScore ) {
                maxScore = score;
                bestPosition = actualPosition;
            }
        }

        return bestPosition;
    }


    private void  fixNode(Node<Span> node, Set<Node<Span>> nodeVisit) {
        if (nodeVisit.contains(node)) {
            //do nothing
            return;
        }
        if (node.getChildren().size() == 0) { // there are no children
            //do nothing; just label as visited
            nodeVisit.add(node);
        } else if (node.getChildren().size() == 1) { // there is only one child
            nodeVisit.add(node);
            for (Node<Span> childI : node.getChildren()) {
                fixNode(childI, nodeVisit);
            }
        } else { // there are at least two children

            Iterator<Node<Span>> iteratorI = node.getChildren().iterator();
            Node<Span> childI;
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
                                modifiedI = modifiedI | posI.remove(pos);
                                if (!modifiedI) {
                                    modifiedJ = modifiedJ | posJ.remove(pos);
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

        if ( nodePositions.size() == 0  ) {
            if (positionsToRemove.contains(node.getData().getAnchor())) {
                node.getData().setAnchor(-1);
            }
        } else {
            node.getData().setAnchor(nodePositions.get(0));
        }

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
            } else {
                if (positionsToRemove.contains(child.getData().getAnchor())) {
                    child.getData().setAnchor(-1);
                }
            }
        }
        for (Node<Span> child : childrenToRemove) {
            node.removeChild(child);
            node.getParent().addChild(child);
        }
    }

    private Node<Span> cloneTree(List<Span> targetSpans, Node<Span> sourceNode) {
        Node<Span> cloneNode = new Node<>(targetSpans.get(sourceNode.getData().getId()));
        for (Node<Span> sourceChild : sourceNode.getChildren()){
            Node<Span> cloneChild = cloneTree(targetSpans, sourceChild);
            cloneNode.addChild(cloneChild);
        }
        return cloneNode;
    }

    static private ArrayList<Integer> intersection(ArrayList<Integer> posI, ArrayList<Integer> posJ) {
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
        ArrayList<Integer> contiguous = new ArrayList<>(positions.size());
        if (positions.size() > 0) {
            for (int i = Collections.min(positions); i <= Collections.max(positions); i++) {
                contiguous.add(i);
            }
        }
        return contiguous;
    }

    static private int choosePosition(ArrayList<Integer> posI, ArrayList<Integer> posJ) {
        int minI = Collections.min(posI);
        int minJ = Collections.min(posJ);
        int maxI = Collections.max(posI);
        return (minI <= minJ) ? maxI : minJ;
    }

    static private ArrayList<Integer> choosePositions(final ArrayList<Integer> posI, final ArrayList<Integer> posJ) {
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

        //Sort the tag in according to their position and order in the source sentence
        Collections.sort(tags);

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