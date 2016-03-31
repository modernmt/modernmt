package eu.modernmt.processing.framework.string;

import java.util.*;

/**
 * Created by lucamastrostefano on 25/03/16.
 */
public class XMLEditableString {

    protected enum TokenType {
        Word,
        XML
    }

    public static class TokenHook {

        protected int startIndex;
        protected int length;
        protected TokenType tokenType;
        protected String processedString;
        protected boolean hasRightSpace;

        public TokenHook(int startIndex, int length, TokenType tokenType) {
            this.startIndex = startIndex;
            this.length = length;
            this.tokenType = tokenType;
        }

        @Override
        public String toString() {
            return "TokenHook{" +
                    "startIndex=" + startIndex +
                    ", length=" + length +
                    ", tokenType=" + tokenType +
                    ", processedString=" + processedString +
                    '}';
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getLength() {
            return length;
        }

        public TokenType getTokenType() {
            return tokenType;
        }

        public String getProcessedString() {
            return processedString;
        }

        public boolean isHasRightSpace() {
            return hasRightSpace;
        }
    }

    protected static class Operation {

        protected int startIndex;
        protected int length;
        protected int lengthNewString;
        protected String newString;
        protected TokenType tokenType;
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
    private StringEditor stringEditor;
    private boolean compiled;

    protected XMLEditableString(String originalString) {
        this.originalString = originalString;
        this.currentString = new StringBuilder(originalString);
        this.changeLog = new LinkedList<>();
        this.tokens = new ArrayList<>();
        this.xml = new ArrayList<>();
        this.stringEditor = new StringEditor(this);
        this.compiled = false;
    }

    public StringEditor getEditor() {
        if (this.stringEditor.isInUse()) {
            throw new IllegalStateException("An instance of StringEditor is still in use.");
        } else if (this.compiled) {
            throw new IllegalStateException("XMLEditableString already compiled");
        } else {
            this.stringEditor.init();
            return this.stringEditor;
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
            if (TokenType.Word.equals(operation.tokenType)) {
                operation.newString = this.currentString.substring(operation.startIndex, operationEndIndex);
                operation.lengthNewString = operation.newString.length();
            }
            int delta = operation.lengthNewString - operation.length;
            int operationLastEditedIndex = operationEndIndex - 1;
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

            if (TokenType.XML.equals(operation.tokenType)) {
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

            if (!TokenType.Word.equals(operation.tokenType)) {
                this.currentString.replace(operation.startIndex, operationEndIndex, operation.newString);
            }

            if (save) {
                if (operation.tokenType != null) {
                    TokenHook hook = new TokenHook(operation.startIndex, operation.lengthNewString,
                            operation.tokenType);
                    if (TokenType.XML.equals(operation.tokenType)) {
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
            if (!TokenType.Word.equals(operation.tokenType)) {
                Operation inverse = operation.getInverse();
                //inverse.tokenType = null;
                Collection<Operation> c = new LinkedList<>();
                c.add(inverse);
                this.applyOperations(c, false);
            }
        }
    }

    public SortedSet<TokenHook> compile() {
        if (this.compiled) {
            throw new IllegalStateException("XMLEditableString already compiled");
        }
        this.reverseChangeLog();
        this.compiled = true;
        SortedSet<TokenHook> tokenHooks = new TreeSet<>(new Comparator<TokenHook>() {
            @Override
            public int compare(TokenHook t1, TokenHook t2) {
                return t1.startIndex - t2.startIndex;
            }
        });
        tokenHooks.addAll(this.tokens);
        tokenHooks.addAll(this.xml);
        return tokenHooks;
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
}
