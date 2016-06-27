package eu.modernmt.aligner;

import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.util.List;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public interface Aligner extends Closeable {

    void load() throws AlignerException;

    int[][] getAlignment(Sentence sentence, Sentence translation) throws AlignerException;

    int[][][] getAlignments(List<Sentence> sentences, List<Sentence> translations) throws AlignerException;

    static String toString(int[][] alignments) {
        StringBuilder result = new StringBuilder();
        for (int[] alignment : alignments) {
            result.append(alignment[0]);
            result.append("-");
            result.append(alignment[1]);
            result.append(" ");
        }
        return result.deleteCharAt(result.length() - 1).toString();
    }

    static void invertAlignments(int[][] alignments) {
        int tmp;
        for (int[] alignment : alignments) {
            tmp = alignment[0];
            alignment[0] = alignment[1];
            alignment[1] = tmp;
        }
    }

}
