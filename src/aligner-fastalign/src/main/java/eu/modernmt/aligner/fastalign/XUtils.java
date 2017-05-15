package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 12/05/17.
 */
class XUtils {

    public static int toInt(Aligner.SymmetrizationStrategy strategy) {
        switch (strategy) {
            case GROW_DIAGONAL_FINAL_AND:
                return 1;
            case GROW_DIAGONAL:
                return 2;
            case INTERSECT:
                return 3;
            case UNION:
                return 4;
        }

        return 0;
    }

    public static String[] toTokensArray(Sentence sentence) {
        return TokensOutputStream.toTokensArray(sentence, false, true);
    }

    public static Alignment parseAlignment(int[] encoded) {
        if (encoded.length % 2 == 1)
            throw new Error("Invalid native result length: " + encoded.length);

        int size = encoded.length / 2;

        int[] source = new int[size];
        int[] target = new int[size];

        System.arraycopy(encoded, 0, source, 0, size);
        System.arraycopy(encoded, size, target, 0, size);

        return new Alignment(source, target);
    }

}
