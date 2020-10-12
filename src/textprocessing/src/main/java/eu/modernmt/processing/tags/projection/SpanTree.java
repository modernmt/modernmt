package eu.modernmt.processing.tags.projection;

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
            if (parent != null)
                this.getData().setLevel(parent.getData().getLevel()+1);
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

    protected void project(SpanTree sourceTree, SpanCollection sourceSpans) {
        this.root = sourceTree.getRoot().clone(this.spans);

        Set<Node> nodeVisit = new HashSet<>();
        fixNode(this.root, nodeVisit, sourceSpans);
        fixAnchors(this.root, sourceSpans);
    }

    Node getRoot() {
        return this.root;
    }

    static private void fixAnchors(Node node, SpanCollection sourceSpans) {
        Span targetSpan = node.getData();
        targetSpan.setAnchor(computeAnchor(node, sourceSpans, true));

        for (Node child : node.getChildren())
            fixAnchors(child, sourceSpans);
    }

    static private int computeAnchor(Node node, SpanCollection sourceSpans, boolean move) {
        Span span = node.getData();
        int targetAnchor = span.getAnchor();
        if (span.getBeginTag() == null) {
// there is no corresponding opening tag
//            targetAnchor = 0; //TODO: OLD VERSION

            Coverage spanPositions = span.getPositions();
            if (spanPositions.size() > 0) {
                targetAnchor = spanPositions.getMin();
            } else {
                targetAnchor = node.getParent().getData().getAnchor();
            }
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
            if (targetAnchor == -1 || node.getData().getPositions().isEmpty()) {

                Node parent = node.getParent();
                Coverage parentPositions = parent.getData().getPositions();

                if (parentPositions.size() == 0) {
                    //the parent anchor has already been fixed.
                    targetAnchor = parent.getData().getAnchor();
                } else {
                    //the tag can float within the parent positions,
                    //but cannot belong to its sibling (if any)
                    Coverage sourceParentPositions = sourceSpans.get(parent.getId()).getPositions();

                    assert(sourceParentPositions.size() > 0);
                    int sourceAnchor = sourceSpans.get(span.getId()).getAnchor();

                    if (targetAnchor == -1 ) {
                        float ratio = (float) (sourceAnchor - sourceParentPositions.getMin()) / sourceParentPositions.size();
                        targetAnchor = Math.round(parentPositions.getMin() + parentPositions.size() * ratio);
                    }

                    if (move) {
                        //check if the guessed targetAnchor falls into a sibling spanning at least two positions
                        //not in the first position; if it falls in the first position is means that it is position outside (on left)
                        for (Node child : parent.getChildren()) {
                            Coverage coverage = child.getData().getPositions();
                            if ((coverage.size() >= 2) &&
                                    coverage.contains(targetAnchor) &&
                                    (targetAnchor > coverage.getMin())) {
                                targetAnchor = coverage.getMax() + 1;
                                break;
                            }
                        }
                    }
                }
            }
        }

        assert(targetAnchor != -1);
        return targetAnchor;
    }

    static private void fixPositions(Node node, Node child) {
        Coverage positions = Coverage.difference(child.getData().getPositions(), node.getData().getPositions());
        for (Integer pos : positions) {
            // remove the positions from child not included in the node (because already removed)
            child.getData().getPositions().remove(pos);
        }
    }

    static private boolean isArtificial(Node node) {
        return (node.getData().getBeginTag() == null) || (node.getData().getEndTag() == null);
    }

    static private void fixTwoArtificialSiblings(Node childI, Node childJ) {
        if (isArtificial(childI) && isArtificial(childJ) )
            fixSiblings(childI, childJ);
    }


    static private Node fixOneArtificialSiblings(Node childI, Node childJ, SpanCollection sourceSpans) {
        if ( (isArtificial(childI) && isArtificial(childJ))
             || (!isArtificial(childI) && !isArtificial(childJ)) )
            return null;

        Node childToRemove = null;

        Node futureParent;
        Node currentChild;

        if ( isArtificial(childI) ) {
            futureParent = childI;
            currentChild = childJ;
        } else {
            futureParent = childJ;
            currentChild = childI;
        }

        Span parentSpan = futureParent.getData();
        Span childSpan = currentChild.getData();
        Coverage parentPos = Coverage.contiguous(parentSpan.getPositions());
        Coverage childPos = Coverage.contiguous(childSpan.getPositions());
        Coverage positions = Coverage.intersection(parentPos, childPos);

        int targetAnchor = childSpan.getAnchor();
        if (targetAnchor == -1)
            childSpan.setAnchor(computeAnchor(currentChild, sourceSpans, false));

        if ((!positions.isEmpty() && Coverage.difference(childPos, positions).isEmpty())
                || (childPos.isEmpty() && parentPos.contains(targetAnchor) && targetAnchor > parentPos.getMin())) {
            // childPos are completely included in parentPos
            // childPos is empty and the anchor in included in parentPos
            // hence, make currentChild a child of futureParent
            futureParent.addChild(new Node(childSpan));
            childToRemove = currentChild;
        }

        return childToRemove;
    }

    static private void fixStandardSiblings(Node childI, Node childJ) {
        if ( !isArtificial(childI) && !isArtificial(childI) )
            fixSiblings(childI, childJ);
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
                // remove the chosen position from the largest coverage
                if (posI.size() > posJ.size()) {
                    modified = posI.remove(posToRemove) || posJ.remove(posToRemove);
                } else {
                    modified = posJ.remove(posToRemove) || posI.remove(posToRemove);
                }
                if (posI.isEmpty()) {
                    childI.getData().setAnchor(-1);

                } else {
                    childI.getData().setAnchor(posI.getMin());
                }
                if (posJ.isEmpty()) {
                    childJ.getData().setAnchor(-1);
                } else {
                    childJ.getData().setAnchor(posJ.getMin());
                }
            }
        }
    }

    static private void fixNode(Node node, Set<Node> nodeVisit, SpanCollection sourceSpans) {
        if (nodeVisit.contains(node)) {
            //do nothing
            return;
        }
        if (node.getChildren().size() == 0) { // there are no children
            //do nothing; just label as visited
            nodeVisit.add(node);
        } else {
            for (Node child : node.getChildren())
                //remove from child the positions of child not included in node
                fixPositions(node, child);

            if (node.getChildren().size() > 1) { // there are at least two children
                Node childI, childJ;

                Iterator<Node> iteratorI = node.getChildren().iterator();
                while (iteratorI.hasNext()) {
                    childI = iteratorI.next();

                    if (nodeVisit.contains(childI))
                        //childI is already fixed
                        continue;

                    for (Node value : node.getChildren()) {
                        childJ = value;

                        if (childI.getId() >= childJ.getId() || nodeVisit.contains(childJ))
                            //childI and childJ are already considered or childJ is already fixed
                            continue;

                        fixTwoArtificialSiblings(childI, childJ);
                    }
                }

                Set<Node> childrenToRemove = new HashSet<>();
                iteratorI = node.getChildren().iterator();
                while (iteratorI.hasNext()) {
                    childI = iteratorI.next();

                    if (nodeVisit.contains(childI) || childrenToRemove.contains(childI))
                        //childI is already fixed
                        continue;

                    for (Node value : node.getChildren()) {
                        childJ = value;

                        if (childI.getId() >= childJ.getId() || nodeVisit.contains(childJ) || childrenToRemove.contains(childJ))
                            //childI and childJ are already considered or childJ is already fixed ir childJ has to be removed from the chilren
                            continue;

                        Node childToRemove = fixOneArtificialSiblings(childI, childJ, sourceSpans);
                        if (childToRemove != null)
                            childrenToRemove.add(childToRemove);
                    }
                }

                for (Node child : childrenToRemove) {
                    //remove all children which became children of a child
                    node.removeChild(child);
                }

                iteratorI = node.getChildren().iterator();
                while (iteratorI.hasNext()) {
                    childI = iteratorI.next();

                    if (nodeVisit.contains(childI))
                        //childI is already fixed
                        continue;

                    for (Node value : node.getChildren()) {
                        childJ = value;

                        if (childI.getId() >= childJ.getId() || nodeVisit.contains(childJ))
                            //childI and childJ are already considered or childJ is already fixed
                            continue;

                        fixStandardSiblings(childI, childJ);

                    }
                }
            }

            fixAnchors(node, sourceSpans);

            // all children are fixed; label as visited
            nodeVisit.add(node);
            for (Node child : node.getChildren()) {
                //perform fixing on all children
                fixNode(child, nodeVisit, sourceSpans);
            }
        }
    }

    private String toString(Node node) {
        StringBuilder result = new StringBuilder();
        toString(node, result, "");
        return result.toString();
    }

    private void toString(Node node, StringBuilder collector, String indentation) {
        if (node != null) {
            collector.append(indentation).append(node).append('\n');
            node.getChildren().forEach(each -> toString(each, collector, "  " + indentation));
        }
    }

    @Override
    public String toString() {
        return toString(this.root);
    }
}