package eu.modernmt.vocabulary;

import java.io.Closeable;
import java.util.List;

/**
 * Created by davide on 01/09/16.
 */
public interface Vocabulary extends Closeable {

    //TODO: replace Vocabulary with DbVocabulary implements Vocabulary

    int VOCABULARY_UNKNOWN_WORD = 0;
    int VOCABULARY_WORD_ID_START = 1000;

    int lookup(String word, boolean putIfAbsent);

    int[] lookupLine(String[] line, boolean putIfAbsent);

    List<int[]> lookupLines(List<String[]> lines, boolean putIfAbsent);

    int[][] lookupLines(String[][] lines, boolean putIfAbsent);

    String reverseLookup(int id);

    String[] reverseLookupLine(int[] line);

    List<String[]> reverseLookupLines(List<int[]> lines);

    String[][] reverseLookupLines(int[][] lines);

}
