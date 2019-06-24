package eu.modernmt.aligner;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Alignment;
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

    Alignment getAlignment(LanguageDirection direction, Sentence source, Sentence target) throws AlignerException;

    Alignment[] getAlignments(LanguageDirection direction, List<? extends Sentence> sources, List<? extends Sentence> targets) throws AlignerException;

    Alignment getAlignment(LanguageDirection direction, Sentence source, Sentence target, SymmetrizationStrategy strategy) throws AlignerException;

    Alignment[] getAlignments(LanguageDirection direction, List<? extends Sentence> sources, List<? extends Sentence> targets, SymmetrizationStrategy strategy) throws AlignerException;

    boolean isSupported(LanguageDirection direction);

}
