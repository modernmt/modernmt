package eu.modernmt.processing.xml.projection;

import java.util.*;

import static eu.modernmt.model.Tag.Type;

class SpanTree {
    public class Node implements Comparable<Node>{
        private List<Node> children = new ArrayList<>();
        private Node parent = null;
        private Span data;

        Node(Span data) {
            this.data = new Span(data);
        }

        List<Node> getChildren() {
            return children;
        }

        Node getParent() {
            return parent;
        }

        void setParent(Node parent) {
            this.parent = parent;
        }

        void addChild(Node child) {
            child.setParent(this);
            this.children.add(child);
        }

        void removeChild(Node child) {
            // the child remove isolated; its parent is set to null, but it is not added to the children of the root
            child.setParent(null);
            this.children.remove(child);
        }

        public Span getData() {
            return this.data;
        }

        public int getId() {
            return (this.data).getId();
        }

        public void setData(Span data) {
            this.data = data;
        }

        public boolean isRoot() {
            return (this.parent == null);
        }

        boolean isLeaf() {
            return this.children.size() == 0;
        }

        public String toString() {
            StringBuilder str = new StringBuilder("Node");
            if (this.parent == null) {
                str.append(" parent:").append(this.parent);
            } else {
                str.append(" parent:").append(this.parent.getId());
            }
            str.append(" data:").append(this.getData());
            str.append(" children:");
            for (Node child : this.children) {
                str.append(child.getId()).append(",");
            }
            return str.toString();
        }

        void sortChildren() {
            if (!isLeaf()) {
                ArrayList<Node> nodes = new ArrayList<>(this.children);
                nodes.sort(Node::compareTo);

                this.children.clear();
                nodes.forEach(this::addChild);
            }
        }

        @Override
        public int compareTo(Node a) {
            return (this.getData()).compareTo(a.getData());
        }

        Node clone(SpanCollection targetSpans) {
            Node cloned = new Node(targetSpans.get(this.getId()));
            this.children.stream().map(sourceChild -> sourceChild.clone(targetSpans)).forEach(cloned::addChild);
            return cloned;
        }
    }

    private static int ROOT_INDEX = 0;
    private static String INDENTATION = "  ";

    private SpanCollection spans;
    private Node root;

    SpanTree(SpanCollection spans) {
        this.spans = spans;
        this.root = null;
    }

    protected void sort() {
        sort(this.root);
    }

    private static void sort(Node node) {
        node.sortChildren();
        node.getChildren().forEach(SpanTree::sort);
    }

    protected void print() {
        print(this.root);
    }

    protected void print(Node node) {
        print(node, INDENTATION);
    }

    protected void print(Node node, String appender) {
        if (node != null) {
            System.out.println(appender + node.toString());
            node.getChildren().forEach(each -> print(each, INDENTATION + appender));
        }
    }

    protected void create() {
        List<Integer> spanVisit = new ArrayList<>();
        this.root = create(ROOT_INDEX, spanVisit);
        this.sort();
    }

    protected Node create(int spanIdx, List<Integer> spanVisit) {
        Node root = new Node(spans.get(spanIdx));
        int rootLevel = root.getData().getLevel();

        spanVisit.add(spanIdx);

        int firstChildIdx = spanIdx + 1;
        //search for the first span on the right which has level = root.getLevel()+1, but is not child of a sibling
        while (firstChildIdx < spans.size()) {
            if  ((spans.get(firstChildIdx).getLevel() == rootLevel) || (spans.get(firstChildIdx).getLevel() == rootLevel + 1) ) {
                break;
            }
            firstChildIdx++;
        }

        // consider all spans from firstChildIdx to the last span
        int idx = firstChildIdx;
        while (idx < spans.size()) {
            if (!spanVisit.contains(idx)) {
                Span span = spans.get(idx);
                assert (span.getLevel() >= root.getData().getLevel());
                if (span.getLevel() <= root.getData().getLevel()) {
                    break;
                }

                if (span.getLevel() == rootLevel + 1) {
                    Node node = create(idx, spanVisit);
                    root.addChild(node);
                }
            }
            idx++;
        }

        firstChildIdx = spanIdx - 1;
        //search for the first span on the right having level = root.getLevel()+1
        while (firstChildIdx >= 0 && (spans.get(firstChildIdx).getLevel() != rootLevel + 1)) {
            if ( (spans.get(firstChildIdx).getLevel() == rootLevel) || (spans.get(firstChildIdx).getLevel() == rootLevel + 1) ) {
                break;
            }
            firstChildIdx--;
        }

        // consider all spans on the left of the root
        idx = 0;
        while (idx <= firstChildIdx) {

            if (!spanVisit.contains(idx)) {
                Span span = spans.get(idx);
                assert (span.getLevel() >= root.getData().getLevel());
                if (span.getLevel() <= root.getData().getLevel()) {
                    break;
                }

                if (span.getLevel() == rootLevel + 1) {
                    Node node = create(idx, spanVisit);
                    root.addChild(node);
                }
            }
            idx++;
        }
        return root;
    }

    protected void project(SpanTree sourceTree, Alignment alignment, int targetWords) {
        this.root = sourceTree.getRoot().clone(this.spans);

        Set<Node> nodeVisit = new HashSet<>();
        fixNode(this.root, nodeVisit);

        fixAnchors(this.root, alignment, targetWords);

        fixUndefinedAnchors(this.root);
    }

    Node getRoot() {
        return this.root;
    }

    static private void fixAnchors(Node node, Alignment alignment, int targetWords) {
        Span targetSpan = node.getData();
        if (targetSpan.getAnchor() == -1) {
            targetSpan.setAnchor(computeAnchor(node, alignment, targetWords));
        }
        //overwrite anchors of empty tags
        if ((targetSpan.getBeginTag() != null) && (targetSpan.getBeginTag().getType() == Type.EMPTY_TAG)) {
            targetSpan.setAnchor(computeAnchorForSelfClosing(targetSpan.getBeginTag().getPosition(), alignment, targetWords));
        }
        for (Node child : node.getChildren()) {
            fixAnchors(child, alignment, targetWords);
        }
    }

    static private void fixUndefinedAnchors(Node node) {
//        System.out.println("fixUndefinedAnchors on node:" + node);
        if (node.getChildren().size() >= 2) {
            //fix anchor of children having beginTag == null; it assign the anchor of the closest right span with anchors ~= -1
            for (int i = 0; i < node.getChildren().size(); i++) {
                Node childI = node.getChildren().get(i);

                int newAnchor = childI.getData().getAnchor();
                if (newAnchor == -1 && childI.getData().getBeginTag() == null) {

                    for (int j = i + 1; j < node.getChildren().size(); j++) {
                        Node childJ = node.getChildren().get(j);
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
                Node childI = node.getChildren().get(i);

                int newAnchor = childI.getData().getAnchor();
                if (newAnchor == -1 && childI.getData().getEndTag() == null) {

                    for (int j = i - 1; j >= 0; j--) {
                        Node childJ = node.getChildren().get(j);
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
                Node childI = node.getChildren().get(i);

                int anchorI = childI.getData().getAnchor();
                if (anchorI == -1 && childI.getData().getEndTag() != null && childI.getData().getBeginTag() != null) {

                    int leftClosest = 0;
                    int leftAnchor = -1;
                    for (int j = i - 1; j >= 0; j--) {
                        Node childJ = node.getChildren().get(j);
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
                        Node childJ = node.getChildren().get(j);
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

        for (Node child : node.getChildren()) {
            fixUndefinedAnchors(child);
        }
    }

    static private int computeAnchor(Node node, Alignment alignment, int targetWords) {
        int targetAnchor = -1;
        Span span = node.getData();
        if (span.getBeginTag() == null) {
            targetAnchor = 0;
        } else {
            if (span.getPositions().size() > 0) { //node with at least 1 contained token
                targetAnchor = span.getPositions().get(0);
            } else if (span.getBeginTag().getType() == Type.EMPTY_TAG) {
                targetAnchor = computeAnchorForSelfClosing(span.getBeginTag().getPosition(), alignment, targetWords);
            }
        }

        return targetAnchor;
    }

    static private int computeAnchorForSelfClosing(int sourcePosition, Alignment alignment, int targetWords) {
        Coverage sourceLeftToken = new Coverage();
        Coverage sourceRightToken = new Coverage();
        Coverage targetLeftToken = new Coverage();
        Coverage targetRightToken = new Coverage();
        Coverage leftTokenIntersection = new Coverage();
        Coverage rightTokenIntersection = new Coverage();

        //Words that are at the left of the tag in the source sentence, should be at left of the mapped tag
        //in the translation. Some reasoning for those that are at the right.

        for (int sourceP = 0; sourceP < alignment.size(); sourceP++) {
            if (sourceP < sourcePosition) {
                //If the word is at the left of the current tag
                //Remember that it should be at the left also in the translation
                sourceLeftToken.addAll(alignment.get(sourceP));
            } else {
                //Remember that it should be at the right also in the translation
                sourceRightToken.addAll(alignment.get(sourceP));
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

    static private void fixNode(Node node, Set<Node> nodeVisit) {
        if (nodeVisit.contains(node)) {
            //do nothing
            return;
        }
        if (node.getChildren().size() == 0) { // there are no children
            //do nothing; just label as visited
            nodeVisit.add(node);
        } else if (node.getChildren().size() == 1) { // there is only one child
            nodeVisit.add(node);
            for (Node child : node.getChildren()) {
                Coverage positionsToRemove = Coverage.difference(child.getData().getPositions(), node.getData().getPositions());
                fixChildren(node, positionsToRemove);
                fixNode(child, nodeVisit);
            }
        } else { // there are at least two children
            Iterator<Node> iteratorI = node.getChildren().iterator();
            Node childI;
            boolean modifiedI = true;
            boolean modifiedJ = true;
            while (iteratorI.hasNext()) {
                modifiedI = false;
                modifiedJ = false;
                childI = iteratorI.next();
                Node childJ = null;

                if (nodeVisit.contains(childI)) {
                    continue;   //childI and its descendant are already fixed
                }
                Coverage positionsToRemove = Coverage.difference(childI.getData().getPositions(), node.getData().getPositions());
                fixChildren(node, positionsToRemove);
                positionsToRemove.clear();

                Iterator<Node> iteratorJ = node.getChildren().iterator();
                while (!modifiedI && !modifiedJ && iteratorJ.hasNext()) {
                    childJ = iteratorJ.next();

                    if (childI != childJ) {
                        Coverage posI = childI.getData().getPositions();
                        Coverage posJ = childJ.getData().getPositions();

                        Coverage contiguousIntersection = Coverage.intersection(Coverage.contiguous(posI), Coverage.contiguous(posJ));
                        if (!contiguousIntersection.isEmpty()) {
                            // the two children overlap
                            // choose one point to exclude from both childI and childJ
                            // so that their intersection is minimal

                            positionsToRemove = Coverage.choosePositions(posI, posJ);

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

    static private void fixChildren(Node node, Coverage positionsToRemove) {
        Coverage nodePositions = node.getData().getPositions();

        if ( nodePositions.size() == 0  ) {
            if (positionsToRemove.contains(node.getData().getAnchor())) {
                node.getData().setAnchor(-1);
            }
        } else {
            node.getData().setAnchor(nodePositions.get(0));
        }

        List<Node> childrenToRemove = new ArrayList<>();
        for (Node child : node.getChildren()) {
            Coverage childPositions = node.getData().getPositions();

            if ( childPositions.size() > 0 ) {
                Coverage intersection = Coverage.intersection(childPositions, nodePositions);
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
        for (Node child : childrenToRemove) {
            node.removeChild(child);
            node.getParent().addChild(child);
        }
    }


}