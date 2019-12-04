package eu.modernmt.processing.tags.projection;

import eu.modernmt.model.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TagCollection implements Iterable<Tag> {
    private List<Tag> list;


    TagCollection() {
        this.list = new ArrayList<>(0);
    }

    TagCollection(Tag[] tags) {
        this.list = new ArrayList<>(tags.length);
        this.list.addAll(Arrays.asList(tags));
    }


    void populate(SpanTree tree) {
        populate(tree.getRoot());

        //Sort the tag in according to their position and order in the source sentence
        Collections.sort(this.list);
    }

    private void populate(SpanTree.Node node) {
        Span span = node.getData();

        if (span.getBeginTag() != null) {
            int anchor = (span.getAnchor() == -1) ? 0 : span.getAnchor();
            Tag t = span.getBeginTag();
            t.setPosition(anchor);
            this.list.add(t);
        }

        node.getChildren().forEach(this::populate);

        if (span.getEndTag() != null && span.getBeginTag() != span.getEndTag()) {
            int anchor;
            if (span.getPositions().size() > 0) {
                anchor = span.getPositions().getMax() + 1;
            } else {
                anchor = (span.getAnchor() == -1) ? 0 : span.getAnchor();
            }

            Tag t = span.getEndTag();
            t.setPosition(anchor);
            this.list.add(t);
        }
    }

    protected void print() {
        int tagIdx = 0;
        for (Tag tag : this.list) {
            System.out.println("Tag " + tagIdx + " " + tag.toString() + " " + tag.getType() + " " + tag.getPosition());
            tagIdx++;
        }
    }

    void fixXmlCompliance() {
        //fix corrupted xml
        // scans from left to right,
        // searches for the first corrupting closing tag
        // resets this tag and (optionally) its corresponding opening tag as self-closing
        // Scanning from right to left gives different output
        TagCollection inspectTags = new TagCollection();
        for (Tag tag : this.list) {
            String name = tag.getName();
            Tag.Type type = tag.getType();
            if (type == Tag.Type.OPENING_TAG) {
                inspectTags.add(tag);
            }
            if (type == Tag.Type.CLOSING_TAG) {
                if (!inspectTags.isEmpty()) {
                    int lastIndex = inspectTags.size() - 1;
                    if (inspectTags.get(lastIndex).getName().equals(name)) {
                        inspectTags.remove(lastIndex);
                    } else {
                        fixTags(inspectTags, tag);
                    }
                }
            }
        }
    }

    private boolean isEmpty() {
        return this.list.isEmpty();
    }

    private void fixTags(TagCollection inspectTags, Tag tag) {
        //search the right opening tag
        Tag cursorTag = null;
        boolean found = false;

        int idx = inspectTags.size() - 1;
        while (!found && idx >= 0) {
            cursorTag = inspectTags.get(idx);
            if (cursorTag.getName().equals(tag.getName())) {
                found = true;
                break;
            }
            idx--;
        }

        //set the tags  and  the cursor (if found) as empty tags
        tag.setType(Tag.Type.EMPTY_TAG);
        if (found) {
            cursorTag.setType(Tag.Type.EMPTY_TAG);
            inspectTags.remove(cursorTag);
        }
    }


    protected void add(Tag tag) {
        this.list.add(tag);
    }

    protected void remove(Tag tag) {
        this.list.remove(tag);
    }

    private void remove(int idx) {
        this.list.remove(idx);
    }

    protected Tag get(int i) {
        return list.get(i);
    }

    int size() {
        return this.list.size();
    }

    public Tag[] getTags() {
        return this.isEmpty() ? null : this.list.toArray(new Tag[0]);
    }


    @NotNull
    @Override
    public Iterator<Tag> iterator() {
        return new Iterator<Tag>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < list.size();
            }

            @Override
            public Tag next() {
                Tag pos = list.get(index);
                index++;
                return pos;
            }
        };
    }
}
