package eu.modernmt.processing.framework.string;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Created by lucamastrostefano on 25/03/16.
 */
public class ProcessedString {

    protected static class Operation {

        protected int startIndex;
        protected int length;
        protected int lengthNewString;
        protected String newString;
        private String originalString;

        @Override
        public String toString() {
            return "Operation{" +
                    "startIndex=" + startIndex +
                    ", length=" + length +
                    ", lengthNewString=" + lengthNewString +
                    ", newString='" + newString + '\'' +
                    ", originalString='" + originalString + '\'' +
                    '}';
        }
    }

    private StringBuilder currentString;
    private Deque<Operation> changeLog;
    private TreeSet<Token> tokens;
    private StringEditor stringEditor;

    public ProcessedString(String originalString) {
        this.currentString = new StringBuilder(originalString);
        this.changeLog = new LinkedList<>();
        this.tokens = new TreeSet<>();
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

    protected TreeSet<Token> getTokens() {
        return this.tokens;
    }

    protected void applyOperations(Collection<Operation> operations) {
        for (Operation operation : operations) {
            int endIndex = operation.startIndex + operation.length;
            operation.originalString = this.currentString.substring(operation.startIndex, endIndex);
            this.currentString.replace(operation.startIndex, endIndex, operation.newString);
            this.changeLog.addLast(operation);
        }
    }

    public Sentence getSentence() {
        return new Sentence(new Token[]{new Token(this.getCurrentString())});
    }

    public Collection<Operation> getChangeLog() {
        return this.changeLog;
    }

    public char[] toCharArray() {
        int l = currentString.length();
        char[] buffer = new char[l];
        currentString.getChars(0, l, buffer, 0);

        return buffer;
    }

    @Override
    public String toString() {
        return currentString.toString();
    }
}
