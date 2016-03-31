package eu.modernmt.engine.training.cleaning;

import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.processing.util.RareCharsNormalizer;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by davide on 14/03/16.
 */
public class DraftFilter implements BilingualCorpusFilter {

    protected static class Signature {

        private static final RareCharsNormalizer NORMALIZER = new RareCharsNormalizer();

        private long signature;

        public static Signature fromPair(BilingualCorpus.StringPair pair) {
            //TODO: preprocess string?
//            String source = NORMALIZER.call(pair.source);
            String source = pair.source;
            int length = source.length();

            String sx, dx;

            if (length > 1) {
                int hlen = length / 2;

                sx = source.substring(0, hlen);
                dx = source.substring(hlen, length);
            } else {
                sx = source;
                dx = "";
            }

            return new Signature((long) (sx.hashCode()) << 32 | (dx.hashCode()) & 0xFFFFFFFFL);
        }

        private Signature(long signature) {
            this.signature = signature;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Signature signature1 = (Signature) o;

            return signature == signature1.signature;

        }

        @Override
        public int hashCode() {
            return (int) (signature ^ (signature >>> 32));
        }

        @Override
        public String toString() {
            return Long.toString(signature);
        }
    }

    private final HashMap<Signature, PseudoDate> filter = new HashMap<>();

    @Override
    public FilterInitializer getInitializer() {
        filter.clear();

        return (corpus, pair) -> {
            Signature signature = Signature.fromPair(pair);
            PseudoDate existent = filter.get(signature);

            if (existent == null) {
                filter.put(signature, new PseudoDate(pair.timestamp));
            } else {
                existent.registerUpdate(pair.timestamp);
            }
        };
    }

    @Override
    public boolean accept(BilingualCorpus.StringPair pair) throws IOException {
        Signature signature = Signature.fromPair(pair);
        PseudoDate timestamp = filter.get(signature);

        return timestamp.match(pair.timestamp);
    }
}
