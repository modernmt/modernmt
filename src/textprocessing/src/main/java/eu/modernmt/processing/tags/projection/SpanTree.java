package eu.modernmt.processing.tags.projection;

import eu.modernmt.model.Tag;

import java.util.*;

class SpanTree {

    public static class Node implements Comparable<Node> {
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
            if ((spans.get(firstChildIdx).getLevel() == rootLevel) || (spans.get(firstChildIdx).getLevel() == rootLevel + 1)) {
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
            if ((spans.get(firstChildIdx).getLevel() == rootLevel) || (spans.get(firstChildIdx).getLevel() == rootLevel + 1)) {
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

    protected void project(SpanTree sourceTree, SpanCollection sourceSpans, Alignment alignment, int targetWords) {
        this.root = sourceTree.getRoot().clone(this.spans);

        Set<Node> nodeVisit = new HashSet<>();
        fixNode(this.root, nodeVisit);
        fixAnchors(this.root, sourceSpans, alignment, targetWords);
//        fixUndefinedAnchors(this.root);
    }

    Node getRoot() {
        return this.root;
    }

    static private void fixAnchors(Node node, SpanCollection sourceSpans, Alignment alignment, int targetWords) {
        Span targetSpan = node.getData();
        targetSpan.setAnchor(computeAnchor(node, sourceSpans, alignment, targetWords));

        for (Node child : node.getChildren()) {
            fixAnchors(child, sourceSpans, alignment, targetWords);
        }
    }

/*
    static private void fixUndefinedAnchors(Node node) {
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
*/

    static private int computeAnchor(Node node, SpanCollection sourceSpans, Alignment alignment, int targetWords) {
        Span span = node.getData();
        int targetAnchor = span.getAnchor();
        if (span.getBeginTag() == null) {
// there is no corresponding opening tag
            targetAnchor = 0;
        } else if (span.getEndTag() == null) {
// there is no corresponding closing tag
            if (targetAnchor == -1) {
                Coverage spanPositions = span.getPositions();
                if (spanPositions.size() > 0) {
                    targetAnchor = spanPositions.getMin();
                } else {
                    targetAnchor = node.getParent().getData().getAnchor();
                }
            }
        } else {
// there are both opening and closing tags
            if (targetAnchor == -1) {
                Coverage spanPositions = span.getPositions();
                Node parent = node.getParent();

                if (span.getBeginTag().getType() == Tag.Type.EMPTY_TAG) {
                    //the tag can float in any position within its first not empty descendant
                    Coverage parentPositions;
                    while (true) {
                        parentPositions = parent.getData().getPositions();
                        if (!parentPositions.isEmpty())
                            break;
                        parent = node.getParent();
                    }
                    //TODO: to get from SourceTree
                    Coverage sourcePositions = sourceSpans.get(parent.getId()).getPositions();
                    int sourceAnchor = sourceSpans.get(span.getId()).getAnchor();
                    float ratio = (float) (sourceAnchor - sourcePositions.getMin()) / sourcePositions.size();
                    targetAnchor = Math.round(parentPositions.getMin() + parentPositions.size() * ratio);
                } else {
                    //the tag is positioned at the beginning of the parent
                    //i.e it inherits the anchor of the parent
                    targetAnchor = parent.getData().getAnchor();
                }
            }
        }

        return targetAnchor;
    }

    static private int computeSelfClosingAnchor(Node node, Alignment alignment, int targetWords) {
        return 0; //TODO
    }

    static private void fixPositions(Node node, Node child) {
        Coverage positions = Coverage.difference(child.getData().getPositions(), node.getData().getPositions());
        for (Integer pos : positions) {
            // remove the positions from child not included in the node (because already removed)
            child.getData().getPositions().remove(pos);
        }
    }

    static private void fixSiblings(Node childI, Node childJ) {

        boolean modified = true;

        while (modified) {
            modified = false;
            Coverage posI = childI.getData().getPositions();
            Coverage posJ = childJ.getData().getPositions();
            Coverage positions = Coverage.intersection(Coverage.contiguous(posI), Coverage.contiguous(posJ));
            if (!positions.isEmpty()) {
                // the two children overlap
                // choose one point to exclude from either childI or childJ
                // so that their intersection is minimal

                int posToRemove = Coverage.choosePosition(posI, posJ);
                // remove the chosen position from the larget coverage
                if (posI.size() > posJ.size()) {
                    modified = posI.remove(posToRemove) || posJ.remove(posToRemove);
                } else {
                    modified = posJ.remove(posToRemove) || posI.remove(posToRemove);

                }
            }
        }
    }

    static private void fixNode(Node node, Set<Node> nodeVisit) {
        if (nodeVisit.contains(node)) {
            //do nothing
            return;
        }
        if (node.getChildren().size() == 0) { // there are no children
            //do nothing; just label as visited
            nodeVisit.add(node);
        } else {
            for (Node child : node.getChildren()) {
                //remove position of child not included in node
                fixPositions(node, child);
            }
            if (node.getChildren().size() > 1) { // there are at least two children
                Iterator<Node> iteratorI = node.getChildren().iterator();
                Node childI, childJ;
                while (iteratorI.hasNext()) {
                    childI = iteratorI.next();

                    if (nodeVisit.contains(childI)) {
                        continue;   //childI is already fixed
                    }

                    for (Node value : node.getChildren()) {
                        childJ = value;

                        if (childI.getId() >= childJ.getId()) { //childI and childJ are already considered
                            continue;
                        }

                        if (nodeVisit.contains(childJ)) {
                            continue;   //childJ is already fixed
                        }

                        fixSiblings(childI, childJ);
                    }
                }
            }

            // all children are fixed; label as visited
            nodeVisit.add(node);
            for (Node child : node.getChildren()) {
                //perform fixing on all children
                fixNode(child, nodeVisit);
            }
        }
    }
}