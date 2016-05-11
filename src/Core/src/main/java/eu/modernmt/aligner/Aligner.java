package eu.modernmt.aligner;

import eu.modernmt.model.Sentence;

import java.io.Closeable;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public interface Aligner extends Closeable {

    void load() throws AlignerException;

    int[][] getAlignments(Sentence sentence, Sentence translation) throws AlignerException;

    public static String toString(int[][] alignments) {
        StringBuilder result = new StringBuilder();
        for (int[] alignment : alignments) {
            result.append(alignment[0]);
            result.append("-");
            result.append(alignment[1]);
            result.append(" ");
        }
        return result.deleteCharAt(result.length() - 1).toString();
    }

}
