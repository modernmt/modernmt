package eu.modernmt.processing.framework.string;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by lucamastrostefano on 25/03/16.
 */
public class StringEditor {

    private List<XMLEditableString.Operation> changeLog;
    private XMLEditableString xmlEditableString;
    private int lastEditedIndex;
    private int deltaIndexes;
    private boolean inUse;

    protected StringEditor(XMLEditableString XMLEditableString) {
        this.xmlEditableString = XMLEditableString;
    }

    protected void init() {
        this.changeLog = new LinkedList<>();
        this.lastEditedIndex = -1;
        this.deltaIndexes = 0;
        this.inUse = true;
    }

    public void replace(int startIndex, int length, String replace, XMLEditableString.TokenType tokenType) {
        if (!this.inUse) {
            throw new RuntimeException("Closed editor");
        }
        if (startIndex > this.lastEditedIndex) {
            XMLEditableString.Operation operation = new XMLEditableString.Operation();
            operation.startIndex = startIndex + this.deltaIndexes;
            operation.length = length;
            if (replace == null) {
                operation.lengthNewString = length;
            } else {
                operation.newString = replace;
                operation.lengthNewString = replace.length();
            }
            operation.tokenType = tokenType;
            this.changeLog.add(operation);
            this.lastEditedIndex = startIndex + length - 1;
            this.deltaIndexes += (operation.lengthNewString - operation.length);
        } else {
            throw new InvalidOperationException(startIndex, this.lastEditedIndex);
        }
    }

    public void replace(int startIndex, int length, String string) {
        this.replace(startIndex, length, string, null);
    }

    public void delete(int startIndex, int length) {
        this.replace(startIndex, length, "");
    }

    public void insert(int startIndex, String string) {
        this.replace(startIndex, 0, string);
    }

    public void setWord(int startIndex, int length) {
        replace(startIndex, length, null, XMLEditableString.TokenType.Word);
    }

    public void setXMLTag(int startIndex, int length) {
        replace(startIndex, length, " ", XMLEditableString.TokenType.XML);
    }

    public XMLEditableString commitChanges() {
        this.xmlEditableString.applyOperations(this.changeLog);
        this.changeLog = null;
        this.inUse = false;

        return this.xmlEditableString;
    }

    public XMLEditableString discardChanges() {
        this.changeLog = null;
        this.inUse = false;

        return this.xmlEditableString;
    }

    protected boolean isInUse() {
        return inUse;
    }
}

