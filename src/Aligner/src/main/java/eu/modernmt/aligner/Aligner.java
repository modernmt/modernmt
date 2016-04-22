package eu.modernmt.aligner;

import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public interface Aligner extends Closeable {

    void init() throws IOException, ParseException;

    int[][] getAlignments(Sentence sentence, Sentence translation) throws AlignerException;

    void setSymmetrizationStrategy(Symmetrisation.Strategy strategy);

}
