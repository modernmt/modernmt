package eu.modernmt.processing.xml.projection;


import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class Node<T>  implements Comparable<Node<T>>{
    private List<Node<T>> children = new ArrayList<Node<T>>();
    private Node<T> parent = null;
    private T data = null;

    public Node(T data) {
        this.data = data;
    }

    public Node(T data, Node<T> parent) {
        this.data = data;
        this.setParent(parent);
    }

    public List<Node<T>> getChildren() {
        return children;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node<T> parent) {
        this.parent = parent;
    }

    public void addChild(T data) {
        Node<T> child = new Node<T>(data);
        this.addChild(child);
    }

    public void addChild(Node<T> child) {
        child.setParent(this);
        this.children.add(child);
    }

    public void removeChild(Node<T> child) {
        // the child remove isolated; its parent is set to null, but it is not added to the children of the root
        child.setParent(null);
        this.children.remove(child);
    }

    public T getData() {
        return this.data;
    }


    public int getId() {
        return ((Span) this.data).getId();
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isRoot() {
        return (this.parent == null);
    }

    public boolean isLeaf() {
        return this.children.size() == 0;
    }

    public void removeParent() {
        this.parent = null;
    }

    public Node getRoot() {
        if(parent == null){
            return this;
        }
        return parent.getRoot();
    }

    public String toString() {
        String str = "Node";
        if (this.parent == null) {
            str += " parent:" + this.parent;
        } else {
            str += " parent:" + ((Span) this.parent.getData()).getId();
        }
        str += " data:" + this.getData();
        str += " children:";
        for (Node<T> child : this.children) {
            str += ((Span) child.getData()).getId() + ",";
        }
        return str;
    }

    public Node<T> getNode(int index) {
        Node<T> found = null;
        if (this.getId() == index) {
            found = this;
        } else {
            for (Node<T> child : this.children) {
                found = child.getNode(index);
                if (found != null)
                    break;
            }
        }
        return found;
    }

    public void sortChildren() {
        if (children.size() == 0) {
            return;
        }

        ArrayList<Node> nodes = new ArrayList<>(children.size());
        List<Node<T>> tmpChildren = getChildren();
        for (Node<T> child : tmpChildren) {
            child.sortChildren();
            nodes.add(child);
        }

        this.children.clear();
        nodes.sort(Node::compareTo);

        for (Node<T> node : nodes) {
            this.addChild(node);
        }
    }


    @Override
    public int compareTo(@NotNull Node<T> a) {
        return ((Span) this.getData()).compareTo((Span) a.getData());
    }
}