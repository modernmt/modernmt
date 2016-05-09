package eu.modernmt.aligner;

import eu.modernmt.model.Sentence;

import java.io.Closeable;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public interface Aligner extends Closeable {

    void load() throws AlignerException;

    int[][] getAlignments(Sentence sentence, Sentence translation) throws AlignerException;

}
