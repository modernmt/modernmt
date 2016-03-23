package eu.modernmt.aligner.fastalign;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by lucamastrostefano on 16/03/16.
 */
public class ForceTranslation {

    private static final Logger logger = LogManager.getLogger(ForceTranslation.class);

    private enum Operation {
        NULL(0),
        PROTECTED_INSERT(0),
        DELETE(1),
        INSERT(1),
        REPLACE(1);

        final int cost;

        Operation(int cost) {
            this.cost = cost;
        }
    }

    private static class Solution implements Comparable<Solution> {
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
            while (--repetition >= 0) {
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
            return this.cost == ((Solution) obj).cost;
        }
    }

    private static boolean[] getProtectedChars(Sentence translation) {
        boolean[] protectedChars = new boolean[translation.toString().length() * 10];
        int charIndex = 0;
        for (Token token : translation) {
            if (token instanceof Tag) {
                for (int i = 0; i < token.getText().length(); i++) {
                    protectedChars[charIndex++] = true;
                }
            } else {
                for (int i = 0; i < token.getText().length(); i++) {
                    protectedChars[charIndex++] = false;
                }
            }
            if (token.hasRightSpace()) {
                protectedChars[charIndex++] = false;
            }
        }
        return protectedChars;
    }

    public static String forceTranslationAndPreserveTags(Translation fromTranslation, String target) {
        String from = fromTranslation.toString();
        boolean[] protectedChars = getProtectedChars(fromTranslation);

        logger.debug("Computing mapping from: \"" + from + "\" to \"" + target + "\"");
        if (from.equals(target)) {
            return target;
        }
        List<Operation> operations = getMinSetOfOperations(from, target, protectedChars);
        logger.debug("Operation: " + operations);
        logger.debug("Forcing translation to be equal to the original one");

        int fromCharIndex = 0;
        int targetCharIndex = 0;
        StringBuilder targetWithTags = new StringBuilder();
        for (Operation operation : operations) {
            switch (operation) {
                case NULL:
                    targetWithTags.append(target.charAt(targetCharIndex));
                    fromCharIndex++;
                    targetCharIndex++;
                    break;
                case PROTECTED_INSERT:
                    targetWithTags.append(from.charAt(fromCharIndex));
                    fromCharIndex++;
                    break;
                case INSERT:
                    targetWithTags.append(target.charAt(targetCharIndex));
                    targetCharIndex++;
                    break;
                case DELETE:
                    fromCharIndex++;
                    break;
                case REPLACE:
                    targetWithTags.append(target.charAt(targetCharIndex));
                    fromCharIndex++;
                    targetCharIndex++;
                    break;
            }
        }
        return targetWithTags.toString();
    }

    private static List<Operation> getMinSetOfOperations(String from, String target, boolean[] protectedChars) {
        final Solution[][] cache = new Solution[from.length() + 1][target.length() + 1];
        Solution solution = getMinSetOfOperations(from, target, 0, protectedChars, cache);
        return solution.operations;
    }

    private static Solution getMinSetOfOperations(String from, String target, int fromCharIndex, boolean[] protectedChars, Solution[][] cache) {
        int fromLength = from.length();
        int targetLength = target.length();
        if (cache[fromLength][targetLength] != null) {
            return cache[fromLength][targetLength];
        }

        Solution result;
        if (fromLength == 0 && targetLength == 0) {
            result = new Solution(0);
        } else if (fromLength == 0 && targetLength > 0) {
            int cost = Math.max(fromLength, targetLength);
            result = new Solution(cost, Operation.INSERT, cost);
        } else if (protectedChars[fromCharIndex]) {
            //PROTECTED_INSERT
            Solution bestNextSolution = getMinSetOfOperations(from.substring(1, fromLength),
                    target, fromCharIndex + 1, protectedChars, cache);
            LinkedList<Operation> operations = new LinkedList<>(bestNextSolution.operations);
            operations.addFirst(Operation.PROTECTED_INSERT);
            result = new Solution(bestNextSolution.cost + Operation.PROTECTED_INSERT.cost, operations);
        } else if (targetLength > 0 && from.charAt(0) == target.charAt(0)) {
            //DO_NOTHING
            Solution bestNextSolution = getMinSetOfOperations(from.substring(1, fromLength),
                    target.substring(1, targetLength), fromCharIndex + 1, protectedChars, cache);
            LinkedList<Operation> operations = new LinkedList<>(bestNextSolution.operations);
            operations.addFirst(Operation.NULL);
            result = new Solution(bestNextSolution.cost + Operation.NULL.cost, operations);
        } else {
            TreeSet<Solution> possibleSolutions = new TreeSet<>();
            LinkedList<Operation> operations;

            //DELETE
            Solution insertSolution = getMinSetOfOperations(from.substring(1, fromLength),
                    target, fromCharIndex + 1, protectedChars, cache);
            operations = new LinkedList<>(insertSolution.operations);
            operations.addFirst(Operation.DELETE);
            Solution deleteResult = new Solution(insertSolution.cost + Operation.DELETE.cost, operations);
            possibleSolutions.add(deleteResult);

            if (targetLength > 0) {
                //INSERT
                Solution deleteSolution = getMinSetOfOperations(from, target.substring(1, targetLength),
                        fromCharIndex, protectedChars, cache);
                operations = new LinkedList<>(deleteSolution.operations);
                operations.addFirst(Operation.INSERT);
                Solution insertResult = new Solution(deleteSolution.cost + Operation.INSERT.cost, operations);
                possibleSolutions.add(insertResult);

                //REPLACE
                Solution replaceSolution = getMinSetOfOperations(from.substring(1, fromLength),
                        target.substring(1, targetLength), fromCharIndex + 1, protectedChars, cache);
                operations = new LinkedList<>(replaceSolution.operations);
                operations.addFirst(Operation.REPLACE);
                Solution replaceResult = new Solution(replaceSolution.cost + Operation.REPLACE.cost, operations);
                possibleSolutions.add(replaceResult);
            }

            result = possibleSolutions.first();
        }
        cache[fromLength][targetLength] = result;
        return result;
    }


    public static void main(String[] args) throws ProcessingException {
        String from = "<br>ciao, <b id=\"due\">&apos;primo&apos;<b id=\"due\"> test<br>a";
        String target = "ciao,\t`primo` test";

        Sentence preprocessedTranslation = Preprocessor.getPipeline(Locale.forLanguageTag("it"), true).process(from);
        System.out.println(target);
        System.out.println(preprocessedTranslation);
        Translation translation = new Translation(preprocessedTranslation.getWords(), preprocessedTranslation.getTags(),
                null, null);

        System.out.println(ForceTranslation.forceTranslationAndPreserveTags(translation, target));
        System.exit(0);
    }

}
