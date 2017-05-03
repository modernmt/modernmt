package eu.modernmt.rest.framework.routing;

import eu.modernmt.rest.framework.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;

public class RouteTree {

    private Node root = new Node();

    public RouteTree() {
        root = new Node();
    }

    public void add(RouteTemplate route) {
        this.add(root, route, 0);
    }

    private void add(Node parent, RouteTemplate template, int depth) {
        String token = template.getTokenAt(depth);

        if (token == null) {
            parent.setValue(template);
            return;
        }

        boolean isVar = template.isTokenVariable(token);

        String key = isVar ? "*" : token;

        Node child = parent.childs.computeIfAbsent(key, k -> new Node());

        if (template.size() - 1 == depth)
            child.setValue(template);
        else
            this.add(child, template, depth + 1);
    }

    public RouteTemplate get(HttpMethod method, String path) {
        ArrayList<Node> nodes = new ArrayList<>();
        ArrayList<Node> newNodes = new ArrayList<>();
        nodes.add(root);

        for (String token : RouteTemplate.tokenize(path)) {
            newNodes.clear();
            for (Node node : nodes) {
                Node child = node.childs.get(token);
                if (child != null)
                    newNodes.add(child);
                child = node.childs.get("*");
                if (child != null)
                    newNodes.add(child);
            }

            nodes.clear();
            nodes.addAll(newNodes);
        }

        if (nodes.isEmpty())
            return null;
        else
            return nodes.get(0).getValue(method);
    }

    protected static class Node {

        public final HashMap<String, Node> childs = new HashMap<>();
        private HashMap<HttpMethod, RouteTemplate> values = new HashMap<>();

        public void setValue(RouteTemplate value) {
            this.values.put(value.getMethod(), value);
        }

        public boolean hasValue(HttpMethod method) {
            return values.containsKey(method);
        }

        public RouteTemplate getValue(HttpMethod method) {
            return values.get(method);
        }

    }

    @Override
    public String toString() {
        return this.root.toString();
    }

}
