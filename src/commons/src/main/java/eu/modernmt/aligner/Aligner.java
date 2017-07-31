package eu.modernmt.aligner;

import eu.modernmt.model.Alignment;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;

import java.io.Closeable;
import java.util.List;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public interface Aligner extends Closeable {

    enum SymmetrizationStrategy {
        UNION,
        INTERSECT,
        GROW_DIAGONAL,
        GROW_DIAGONAL_FINAL_AND,
    }

    void setDefaultSymmetrizationStrategy(SymmetrizationStrategy strategy);

    SymmetrizationStrategy getDefaultSymmetrizationStrategy();

    Alignment getAlignment(LanguagePair direction, Sentence source, Sentence target) throws AlignerException;

    Alignment[] getAlignments(LanguagePair direction, List<Sentence> sources, List<Sentence> targets) throws AlignerException;

    Alignment getAlignment(LanguagePair direction, Sentence source, Sentence target, SymmetrizationStrategy strategy) throws AlignerException;

    Alignment[] getAlignments(LanguagePair direction, List<Sentence> sources, List<Sentence> targets, SymmetrizationStrategy strategy) throws AlignerException;

}
