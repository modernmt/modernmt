package eu.modernmt.decoder.neural.memory.lucene.analysis;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;

/**
 * Created by davide on 24/05/17.
 */
public class CustomSimilarity extends DefaultSimilarity {

    @Override
    public float lengthNorm(FieldInvertState state) {
        return 1.f;
    }

    @Override
    public float tf(float freq) {
        return 1.f;
    }

}
