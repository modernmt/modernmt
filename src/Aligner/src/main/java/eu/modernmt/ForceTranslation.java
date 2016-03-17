package eu.modernmt;

import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by lucamastrostefano on 16/03/16.
 */
public class ForceTranslation {

    private static final Logger logger = LogManager.getLogger(ForceTranslation.class);

    private static enum Operation{
        NULL(0),
        DELETE(1),
        INSERT(1),
        REPLACE(1);

        final int cost;

        Operation(int cost) {
            this.cost = cost;
        }
    }

    private static class Solution implements Comparable<Solution>{
        final int cost;
        final List<Operation> operations;

        public Solution(int cost) {
            this(cost, null, 0);
        }

        public Solution(int cost, Operation operation) {
            this(cost, operation, 1);
        }

        public Solution(int cost, Operation operation, int repetition) {
            this.cost = cost;
            this.operations = new ArrayList<>();
            while(--repetition >= 0) {
                this.operations.add(operation);
            }
        }

        public Solution(int cost, List<Operation> operations) {
            this.cost = cost;
            this.operations = operations;
        }

        @Override
        public int compareTo(Solution o) {
            return this.cost - o.cost;
        }

        @Override
        public boolean equals(Object obj) {
            return this.cost == ((Solution)obj).cost;
        }
    }

    public static void forceTranslation(String originalTranslation, Translation postProcessedTranslation){
        Token[] postProcessedTokens = postProcessedTranslation.getWords();
        String postProcessTranslation_str = postProcessedTranslation.getStrippedString(false);
        logger.debug("ORIGINAL: " + originalTranslation + " ||| POST: " + postProcessTranslation_str);
        if(originalTranslation.equals(postProcessTranslation_str)){
            return;
        }
        List<Operation> operations = getMinSetOfOperations(originalTranslation, postProcessTranslation_str);
        logger.debug("Operations: " + operations);
        Token lastToken = null;
        int originalCharIndex = 0;
        StringBuilder newToken = new StringBuilder();
        for(int tokenIndex = 0; tokenIndex < postProcessedTokens.length; tokenIndex++){
            newToken.setLength(0);
            Token token = postProcessedTokens[tokenIndex];
            if(token.hasRightSpace()){
                token.setText(token.getText() + " ");
                token.setRightSpace(false);
            }
            for(int charTokenIndex = 0; charTokenIndex < token.getText().length();){
                char currentChar = token.getText().charAt(charTokenIndex);
                switch (operations.get(originalCharIndex)){
                    case NULL:
                        newToken.append(currentChar);
                        originalCharIndex++;
                        charTokenIndex++;
                        break;
                    case INSERT:
                        newToken.append(originalTranslation.charAt(originalCharIndex));
                        originalCharIndex++;
                        break;
                    case DELETE:
                        charTokenIndex++;
                        break;
                    case REPLACE:
                        newToken.append(originalTranslation.charAt(originalCharIndex));
                        originalCharIndex++;
                        charTokenIndex++;
                        break;
                }
            }
            token.setText(newToken.toString());
            lastToken = token;
        }
        //itererate over the remaining character of the original translation
        if(originalCharIndex < originalTranslation.length()){
            lastToken.setText(lastToken.getText() + originalTranslation.substring(originalCharIndex, originalTranslation.length()));
        }
    }

    private static List<Operation> getMinSetOfOperations(String string, String other){
        final Solution[][] cache = new Solution[string.length() + 1][other.length() + 1];
        Solution solution = getMinSetOfOperations(string, other, cache);
        return solution.operations;
    }

    private static Solution getMinSetOfOperations(String string, String other, Solution[][] cache) {
        int stringLength = string.length();
        int otherLength = other.length();
        if(cache[stringLength][otherLength] != null){
            return cache[stringLength][otherLength];
        }

        Solution result;
        if (stringLength == 0 || otherLength == 0){
            if (stringLength == otherLength){
                result = new Solution(0);
            }else{
                int cost = Math.max(stringLength, otherLength);
                result = new Solution(cost, Operation.DELETE, cost);
            }
        }else if (string.charAt(0) == other.charAt(0)){
            //DO_NOTHING
            Solution bestNextSolution = getMinSetOfOperations(string.substring(1, stringLength),
                    other.substring(1, otherLength), cache);
            LinkedList<Operation> operations = new LinkedList<>(bestNextSolution.operations);
            operations.addFirst(Operation.NULL);
            result = new Solution(bestNextSolution.cost + Operation.NULL.cost, operations);
        }else{
            TreeSet<Solution> possibleSolutions = new TreeSet<>();
            LinkedList<Operation> operations;

            //INSERT
            Solution insertSolution = getMinSetOfOperations(string.substring(1, stringLength),
                    other, cache);
            operations = new LinkedList<>(insertSolution.operations);
            operations.addFirst(Operation.INSERT);
            Solution insertResult = new Solution(insertSolution.cost + Operation.INSERT.cost, operations);
            possibleSolutions.add(insertResult);

            //DELETE
            Solution deleteSolution = getMinSetOfOperations(string, other.substring(1, otherLength),
                    cache);
            operations = new LinkedList<>(deleteSolution.operations);
            operations.addFirst(Operation.DELETE);
            Solution deleteResult = new Solution(deleteSolution.cost + Operation.DELETE.cost, operations);
            possibleSolutions.add(deleteResult);

            //DELETE
            Solution replaceSolution = getMinSetOfOperations(string.substring(1, stringLength), other.substring(1, otherLength),
                    cache);
            operations = new LinkedList<>(replaceSolution.operations);
            operations.addFirst(Operation.REPLACE);
            Solution replaceResult = new Solution(replaceSolution.cost + Operation.REPLACE.cost, operations);
            possibleSolutions.add(replaceResult);

            result = possibleSolutions.first();
        }

        cache[stringLength][otherLength] = result;
        return result;
    }

}
