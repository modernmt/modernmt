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
    private TokenMask tokenMask;
    private boolean compiled;

    protected XMLEditableString(String originalString) {
        this.originalString = originalString;
        this.currentString = new StringBuilder(originalString);
        this.changeLog = new LinkedList<>();
        this.tokens = new ArrayList<>();
        this.xml = new ArrayList<>();
        this.editor = new Editor(this);
        this.tokenMask = null;
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

    protected void applyOperations(Collection<Operation> operations) throws InvalidOperationException {
        applyOperations(operations, true);
    }

    protected void applyOperations(Collection<Operation> operations, boolean save) throws InvalidOperationException {
        if (this.compiled) {
            throw new IllegalStateException("XMLEditableString already compiled");
        }
        for (Operation operation : operations) {
            int operationEndIndex = operation.startIndex + operation.length;
            operation.originalString = this.currentString.substring(operation.startIndex, operationEndIndex);

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
                    if (TokenHook.TokenType.XML.equals(operation.tokenType)) {
                        TokenHook hook = new TokenHook(operation.startIndex, operation.lengthNewString,
                                operation.tokenType);
                        this.xml.add(hook);
                    } else {
                        if (this.tokenMask == null) {
                            this.tokenMask = new TokenMask(this.currentString.length());
                        }

                        this.tokenMask.setToken(operation.startIndex, operation.lengthNewString);
                    }
                }

                this.changeLog.push(operation);
            }
        }
    }

    private void reverseChangeLog() throws InvalidOperationException {
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

    public List<TokenHook> compile() throws InvalidOperationException {
        if (this.compiled) {
            throw new IllegalStateException("XMLEditableString already compiled");
        }

        if (tokenMask != null) {
            for (int[] positions : this.tokenMask) {
                int startPosition = positions[0];
                int length = positions[1];
                TokenHook hook = new TokenHook(startPosition, length, TokenHook.TokenType.Word);
                hook.processedString = this.currentString.substring(startPosition, startPosition + length);
                this.tokens.add(hook);
            }
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

        private Editor(XMLEditableString xmlEditableString) {
            this.xmlEditableString = xmlEditableString;
        }

        protected void init() {
            this.changeLog = new LinkedList<>();
            this.lastEditedIndex = -1;
            this.deltaIndexes = 0;
            this.inUse = true;
        }

        private void replace(int startIndex, int length, String replace,
                             TokenHook.TokenType tokenType) throws InvalidOperationException {
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
                this.changeLog.add(operation);
                this.lastEditedIndex = startIndex + length - 1;
                this.deltaIndexes += (operation.lengthNewString - operation.length);
            } else {
                throw new InvalidOperationException(startIndex, this.lastEditedIndex);
            }
        }

        public void replace(int startIndex, int length, String string) throws InvalidOperationException {
            this.replace(startIndex, length, string, null);
        }

        public void delete(int startIndex, int length) throws InvalidOperationException {
            this.replace(startIndex, length, "", null);
        }

        public void insert(int startIndex, String string) throws InvalidOperationException {
            this.replace(startIndex, 0, string, null);
        }

        public void setWord(int startIndex, int length) throws InvalidOperationException {
            replace(startIndex, length, null, TokenHook.TokenType.Word);
        }

        private void setXMLTag(int startIndex, int length) throws InvalidOperationException {
            replace(startIndex, length, " ", TokenHook.TokenType.XML);
        }

        public XMLEditableString commitChanges() throws InvalidOperationException {
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

        private boolean isInUse() {
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

        public Builder append(char c) {
            this.string.append(c);
            return this;
        }

        public Builder append(String s) {
            this.string.append(s);
            return this;
        }

        public Builder append(char[] chars, int offset, int length) {
            this.string.append(chars, offset, length);
            return this;
        }

        public Builder appendXMLTag(String tag) {
            this.tags.add(new int[]{this.string.length(), tag.length()});
            this.string.append(tag);
            return this;
        }

        public XMLEditableString create() throws InvalidOperationException {
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
