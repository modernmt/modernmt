package eu.modernmt.decoder.opennmt.memory.lucene;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.DefaultSimilarity;

/**
 * Created by davide on 24/05/17.
 */
class CustomSimilarity extends DefaultSimilarity {

    @Override
    public float lengthNorm(FieldInvertState state) {
        return 1.f;
    }

    @Override
    public float tf(float freq) {
        return 1.f;
    }

}
