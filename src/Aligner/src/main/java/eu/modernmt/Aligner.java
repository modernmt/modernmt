package eu.modernmt;

import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public interface Aligner extends Closeable {

    void init() throws IOException, ParseException;

    int[][] getAlignments(Sentence sentence, Sentence translation) throws IOException;

    public static int[][] parseAlignments(String stringAlignments){
        String[] links_str = stringAlignments.split(" ");
        int[][] alignments = new int[links_str.length][];
        for(int i = 0; i < links_str.length; i++){
            String[] alignment = links_str[i].split("-");
            alignments[i] = new int[]{Integer.parseInt(alignment[0]), Integer.parseInt(alignment[1])};
        }
        return alignments;
    }
}
