package eu.modernmt.aligner;

import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.util.List;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public interface Aligner extends Closeable {

    Alignment getAlignment(Sentence source, Sentence target) throws AlignerException;

    Alignment[] getAlignments(List<Sentence> sources, List<Sentence> targets) throws AlignerException;

}
