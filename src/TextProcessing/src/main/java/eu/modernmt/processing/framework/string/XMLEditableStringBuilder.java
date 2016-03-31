package eu.modernmt.processing.framework.string;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by lucamastrostefano on 31/03/16.
 */
public class XMLEditableStringBuilder {

    private StringBuilder string;
    private List<int[]> tags;

    public XMLEditableStringBuilder() {
        this.string = new StringBuilder();
        this.tags = new LinkedList<>();
    }

    public void append(char c) {
        this.string.append(c);
    }

    public void append(String s) {
        this.string.append(s);
    }

    public void append(char[] chars, int offset, int length) {
        this.string.append(chars, offset, length);
    }

    public void appendXMLTag(String tag) {
        this.tags.add(new int[]{this.string.length(), tag.length()});
        this.string.append(tag);
    }

    public XMLEditableString create() {
        XMLEditableString editableString = new XMLEditableString(this.string.toString());
        StringEditor editor = editableString.getEditor();
        for (int[] tag : tags) {
            editor.setXMLTag(tag[0], tag[1]);
        }
        editor.commitChanges();
        return editableString;
    }

}

