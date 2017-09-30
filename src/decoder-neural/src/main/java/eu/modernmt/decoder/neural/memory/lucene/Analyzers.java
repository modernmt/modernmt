package eu.modernmt.decoder.neural.memory.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by davide on 24/05/17.
 */
public class Analyzers {

    private static final int SHINGLE_SIZE = 2;

    private static Analyzer trainAnalyzer = null;
    private static Analyzer hashAnalyzer = null;
    private static Analyzer shortQueryAnalyzer = null;
    private static Analyzer longQueryAnalyzer = null;

    public static Analyzer getTrainAnalyzer() {
        if (trainAnalyzer == null)
            trainAnalyzer = new TrainAnalyzer();
        return trainAnalyzer;
    }

    public static Analyzer getHashAnalyzer() {
        if (hashAnalyzer == null)
            hashAnalyzer = new HashAnalyzer();
        return hashAnalyzer;
    }

    public static Analyzer getShortQueryAnalyzer() {
        if (shortQueryAnalyzer == null)
            shortQueryAnalyzer = new ContentAnalyzer(0, true);
        return shortQueryAnalyzer;
    }

    public static Analyzer getLongQueryAnalyzer() {
        if (longQueryAnalyzer == null)
            longQueryAnalyzer = new ContentAnalyzer(SHINGLE_SIZE, false);
        return longQueryAnalyzer;
    }

    public static final class TrainAnalyzer extends DelegatingAnalyzerWrapper {

        public TrainAnalyzer() {
            super(PER_FIELD_REUSE_STRATEGY);
        }

        @Override
        protected Analyzer getWrappedAnalyzer(String fieldName) {
            if (DocumentBuilder.HASH_FIELD.equals(fieldName))
                return getHashAnalyzer();
            else
                return new ContentAnalyzer(SHINGLE_SIZE, true);
        }

    }

    private static final class HashAnalyzer extends Analyzer {

        @Override
        protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            return new TokenStreamComponents(new WhitespaceTokenizer(reader));
        }

    }

    private static final class ContentAnalyzer extends Analyzer {

        private final int shingleSize;
        private final boolean outputUnigrams;

        public ContentAnalyzer(int shingleSize, boolean outputUnigrams) {
            this.shingleSize = shingleSize;
            this.outputUnigrams = outputUnigrams;
        }

        @Override
        protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
            Tokenizer tokenizer = new WhitespaceTokenizer(reader);
            TokenStream filter;
            filter = new PunctuationFilter(tokenizer);

            if (shingleSize > 0) {
                ShingleFilter shingleFilter = new ShingleFilter(filter, shingleSize, shingleSize);
                shingleFilter.setOutputUnigrams(outputUnigrams);

                filter = shingleFilter;
            }

            return new TokenStreamComponents(tokenizer, filter);
        }

    }

    private static final class PunctuationFilter extends FilteringTokenFilter {

        private static final Pattern REGEX = Pattern.compile("\\p{Punct}");

        private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

        public PunctuationFilter(TokenStream in) {
            super(in);
        }

        @Override
        protected boolean accept() throws IOException {
            return !REGEX.matcher(termAtt).matches();
        }
    }
}
