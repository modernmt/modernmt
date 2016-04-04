package eu.modernmt.processing.framework.string;

import java.util.*;

/**
 * Created by lucamastrostefano on 25/03/16.
 */
public class XMLEditableString {

    protected static class Operation {

        protected int startIndex;
        protected int length;
        protected int lengthNewString;
        protected String newString;
        protected TokenHook.TokenType tokenType;
        protected boolean hasRightSpace;
        private String originalString;

        @Override
        public String toString() {
            return "Operation{" +
                    "startIndex=" + startIndex +
                    ", length=" + length +
                    ", lengthNewString=" + lengthNewString +
                    ", newString='" + newString + '\'' +
                    ", tokenType=" + tokenType +
                    ", originalString='" + originalString + '\'' +
                    '}';
        }

        protected Operation getInverse() {
            Operation inverse = new Operation();
            inverse.startIndex = startIndex;
            inverse.length = lengthNewString;
            inverse.newString = originalString;
            inverse.lengthNewString = length;
            inverse.tokenType = tokenType;
            return inverse;
        }
    }

    private String originalString;
    private StringBuilder currentString;
    private Deque<Operation> changeLog;
    private List<TokenHook> tokens;
    private List<TokenHook> xml;
    private Editor editor;
    private boolean compiled;

    protected XMLEditableString(String originalString) {
        this.originalString = originalString;
        this.currentString = new StringBuilder(originalString);
        this.changeLog = new LinkedList<>();
        this.tokens = new ArrayList<>();
        this.xml = new ArrayList<>();
        this.editor = new Editor(this);
        this.compiled = false;
    }

    public Editor getEditor() {
        if (this.editor.isInUse()) {
            throw new IllegalStateException("An instance of Editor is still in use.");
        } else if (this.compiled) {
            throw new IllegalStateException("XMLEditableString already compiled");
        } else {
            this.editor.init();
            return this.editor;
        }
    }

    protected String getCurrentString() {
        return this.currentString.toString();
    }

    protected void applyOperations(Collection<Operation> operations) {
        applyOperations(operations, true);
    }

    protected void applyOperations(Collection<Operation> operations, boolean save) {
        if (this.compiled) {
            throw new IllegalStateException("XMLEditableString already compiled");
        }
        for (Operation operation : operations) {
            int operationEndIndex = operation.startIndex + operation.length;
            operation.originalString = this.currentString.substring(operation.startIndex, operationEndIndex);

            if (TokenHook.TokenType.Word.equals(operation.tokenType)) {
                operation.newString = this.currentString.substring(operation.startIndex, operationEndIndex);
                operation.lengthNewString = operation.newString.length();
            }

            int delta = operation.lengthNewString - operation.length;
            if (delta != 0) {
                for (TokenHook hook : this.tokens) {
                    int hookLastEditedIndex = hook.startIndex + hook.length - 1;
                    if (hook.startIndex >= operationEndIndex) {
                        hook.startIndex += delta;
                    } else if (hook.startIndex > operation.startIndex) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hook.startIndex == operation.startIndex && operation.length > hook.length) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hook.startIndex == operation.startIndex && operation.length < hook.length) {
                        hook.length += delta;
                    } else if (hook.startIndex == operation.startIndex) {
                        hook.length = operation.lengthNewString;
                    } else if (hookLastEditedIndex >= operationEndIndex) {
                        hook.length += delta;
                    } else if (hookLastEditedIndex >= operation.startIndex) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hookLastEditedIndex < operation.startIndex) {
                        //Do nothing
                    } else {
                        throw new InvalidOperationException(operation, hook, "Unexpected situation");
                    }
                }
            }

            if (TokenHook.TokenType.XML.equals(operation.tokenType)) {
                for (TokenHook hook : this.xml) {
                    int hookLastEditedIndex = hook.startIndex + hook.length - 1;
                    if (hook.startIndex >= operationEndIndex) {
                        hook.startIndex += delta;
                    } else if (hook.startIndex > operation.startIndex) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hook.startIndex == operation.startIndex && operation.length > hook.length) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hook.startIndex == operation.startIndex && operation.length < hook.length) {
                        hook.length += delta;
                    } else if (hook.startIndex == operation.startIndex) {
                        hook.length = operation.lengthNewString;
                    } else if (hookLastEditedIndex >= operationEndIndex) {
                        hook.length += delta;
                    } else if (hookLastEditedIndex >= operation.startIndex) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hookLastEditedIndex < operation.startIndex) {
                        //Do nothing
                    } else {
                        throw new InvalidOperationException(operation, hook, "Unexpected situation");
                    }
                }
            }

            if (!TokenHook.TokenType.Word.equals(operation.tokenType)) {
                this.currentString.replace(operation.startIndex, operationEndIndex, operation.newString);
            }

            if (save) {
                if (operation.tokenType != null) {
                    TokenHook hook = new TokenHook(operation.startIndex, operation.lengthNewString,
                            operation.tokenType);
                    if (TokenHook.TokenType.XML.equals(operation.tokenType)) {
                        //hook.length = operation.length;
                        this.xml.add(hook);
                    } else {
                        hook.processedString = operation.newString;
                        hook.hasRightSpace = operation.hasRightSpace;
                        this.tokens.add(hook);
                    }
                }

                this.changeLog.push(operation);
            }
        }
    }

    private void reverseChangeLog() {
        Deque<Operation> operations = new LinkedList<>(this.changeLog);
        while (!operations.isEmpty()) {
            Operation operation = operations.pop();
            if (!TokenHook.TokenType.Word.equals(operation.tokenType)) {
                Operation inverse = operation.getInverse();
                Collection<Operation> c = new LinkedList<>();
                c.add(inverse);
                this.applyOperations(c, false);
            }
        }
    }

    public List<TokenHook> compile() {
        if (this.compiled) {
            throw new IllegalStateException("XMLEditableString already compiled");
        }
        this.reverseChangeLog();
        this.compiled = true;

        ArrayList<TokenHook> hooks = new ArrayList<>(this.tokens.size() + this.xml.size());
        hooks.addAll(this.tokens);
        hooks.addAll(this.xml);

        Collections.sort(hooks, (t1, t2) -> t1.startIndex - t2.startIndex);

        return hooks;
    }

    public String getOriginalString() {
        return this.originalString;
    }

    public char[] toCharArray() {
        int l = currentString.length();
        char[] buffer = new char[l];
        currentString.getChars(0, l, buffer, 0);

        return buffer;
    }

    @Override
    public String toString() {
        return this.currentString.toString();
    }

    public static class Editor {

        private List<Operation> changeLog;
        private XMLEditableString xmlEditableString;
        private int lastEditedIndex;
        private int deltaIndexes;
        private boolean inUse;

        protected Editor(XMLEditableString xmlEditableString) {
            this.xmlEditableString = xmlEditableString;
        }

        protected void init() {
            this.changeLog = new LinkedList<>();
            this.lastEditedIndex = -1;
            this.deltaIndexes = 0;
            this.inUse = true;
        }

        public void replace(int startIndex, int length, String replace,
                            TokenHook.TokenType tokenType, boolean hasRightSpace) {
            if (!this.inUse) {
                throw new RuntimeException("Closed editor");
            }
            if (startIndex > this.lastEditedIndex) {
                Operation operation = new Operation();
                operation.startIndex = startIndex + this.deltaIndexes;
                operation.length = length;
                if (replace == null) {
                    operation.lengthNewString = length;
                } else {
                    operation.newString = replace;
                    operation.lengthNewString = replace.length();
                }
                operation.tokenType = tokenType;
                operation.hasRightSpace = hasRightSpace;
                this.changeLog.add(operation);
                this.lastEditedIndex = startIndex + length - 1;
                this.deltaIndexes += (operation.lengthNewString - operation.length);
            } else {
                throw new InvalidOperationException(startIndex, this.lastEditedIndex);
            }
        }

        public void replace(int startIndex, int length, String replace, TokenHook.TokenType tokenType) {
            this.replace(startIndex, length, replace, tokenType, false);
        }

        public void replace(int startIndex, int length, String string) {
            this.replace(startIndex, length, string, null, false);
        }

        public void delete(int startIndex, int length) {
            this.replace(startIndex, length, "", null, false);
        }

        public void insert(int startIndex, String string) {
            this.replace(startIndex, 0, string, null, false);
        }

        public void setWord(int startIndex, int length, boolean hasRightSpace) {
            replace(startIndex, length, null, TokenHook.TokenType.Word, hasRightSpace);
        }

        public void setXMLTag(int startIndex, int length) {
            replace(startIndex, length, " ", TokenHook.TokenType.XML, false);
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

    public static class Builder {

        private StringBuilder string;
        private List<int[]> tags;

        public Builder() {
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
            Editor editor = editableString.getEditor();
            for (int[] tag : tags) {
                editor.setXMLTag(tag[0], tag[1]);
            }
            editor.commitChanges();
            return editableString;
        }

    }
}
