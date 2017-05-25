package eu.modernmt.decoder.opennmt.memory.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

/**
 * Created by davide on 24/05/17.
 */
public class Analyzers {

    private static final int SHINGLE_SIZE = 2;

    private static Analyzer trainAnalyzer = null;
    private static Analyzer shortQueryAnalyzer = null;
    private static Analyzer longQueryAnalyzer = null;

    public static Analyzer getTrainAnalyzer() {
        if (trainAnalyzer == null)
            trainAnalyzer = new CustomAnalyzer(SHINGLE_SIZE, true);
        return trainAnalyzer;
    }

    public static Analyzer getShortQueryAnalyzer() {
        if (shortQueryAnalyzer == null)
            shortQueryAnalyzer = new CustomAnalyzer(0, true);
        return shortQueryAnalyzer;
    }

    public static Analyzer getLongQueryAnalyzer() {
        if (longQueryAnalyzer == null)
            longQueryAnalyzer = new CustomAnalyzer(SHINGLE_SIZE, false);
        return longQueryAnalyzer;
    }

    private static class CustomAnalyzer extends Analyzer {

        private final int shingleSize;
        private final boolean outputUnigrams;

        public CustomAnalyzer(int shingleSize, boolean outputUnigrams) {
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

    private static class PunctuationFilter extends FilteringTokenFilter {

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
