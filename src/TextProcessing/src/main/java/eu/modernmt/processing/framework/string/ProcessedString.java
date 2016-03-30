package eu.modernmt.processing.framework.string;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;

import java.util.*;

/**
 * Created by lucamastrostefano on 25/03/16.
 */
public class ProcessedString {

    protected enum TokenType {
        Word,
        XML
    }

    protected static class TokenHook {

        protected int startIndex;
        protected int length;
        protected TokenType tagType;

        public TokenHook(int startIndex, int length, TokenType tagType) {
            this.startIndex = startIndex;
            this.length = length;
            this.tagType = tagType;
        }

        @Override
        public String toString() {
            return "TokenHook{" +
                    "startIndex=" + startIndex +
                    ", length=" + length +
                    ", tagType=" + tagType +
                    '}';
        }
    }

    protected static class Operation {

        protected int startIndex;
        protected int length;
        protected int lengthNewString;
        protected String newString;
        protected TokenType tagType;
        private String originalString;

        @Override
        public String toString() {
            return "Operation{" +
                    "startIndex=" + startIndex +
                    ", length=" + length +
                    ", lengthNewString=" + lengthNewString +
                    ", newString='" + newString + '\'' +
                    ", tagType=" + tagType +
                    ", originalString='" + originalString + '\'' +
                    '}';
        }
    }

    private String orginalString;
    private StringBuilder currentString;
    private Deque<Operation> changeLog;
    private List<TokenHook> tokens;
    private StringEditor stringEditor;

    public ProcessedString(String originalString) {
        this.orginalString = orginalString;
        this.currentString = new StringBuilder(originalString);
        this.changeLog = new LinkedList<>();
        this.tokens = new ArrayList<>();
        this.stringEditor = new StringEditor(this);
    }

    public StringEditor getEditor() {
        if (this.stringEditor.isInUse()) {
            throw new IllegalStateException("An instance of StringEditor is still in use.");
        } else {
            this.stringEditor.init();
            return this.stringEditor;
        }
    }

    protected String getCurrentString() {
        return this.currentString.toString();
    }

    protected void applyOperations(Collection<Operation> operations) {
        for (Operation operation : operations) {
            int operationEndIndex = operation.startIndex + operation.length;
            operation.originalString = this.currentString.substring(operation.startIndex, operationEndIndex);
            this.currentString.replace(operation.startIndex, operationEndIndex, operation.newString);
            int delta = operation.lengthNewString - operation.length;
            if (delta != 0) {
                for (TokenHook hook : this.tokens) {
                    int hookEndIndex = hook.startIndex + hook.length;
                    if (hook.startIndex >= operationEndIndex) {
                        hook.startIndex += delta;
                    } else if (hook.startIndex > operation.startIndex) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hook.startIndex == operation.startIndex && hook.length != operation.length) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hook.startIndex == operation.startIndex) {
                        hook.length = operation.lengthNewString;
                    } else if (hookEndIndex >= operationEndIndex) {
                        hook.length += delta;
                    } else if (hookEndIndex >= operation.startIndex) {
                        throw new InvalidOperationException(operation, hook);
                    } else if (hookEndIndex < operation.startIndex) {
                        //Do nothing
                    } else {
                        throw new InvalidOperationException(operation, hook, "Unexpected situation");
                    }
                }
            }

            if (operation.tagType != null) {
                TokenHook hook = new TokenHook(operation.startIndex, operation.lengthNewString,
                        operation.tagType);
                this.tokens.add(hook);
            }

            this.changeLog.addLast(operation);
        }
    }

    public Sentence getSentence() {
        return new Sentence(new Token[]{new Token(this.currentString.toString())});
    }

    public Collection<Operation> getChangeLog() {
        return this.changeLog;
    }

    public Collection<TokenHook> getTokens() {
        return this.tokens;
    }

    public String getOrginalString() {
        return this.orginalString;
    }

    @Override
    public String toString() {
        return this.currentString.toString();
    }
}
