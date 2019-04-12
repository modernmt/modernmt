package eu.modernmt.decoder.neural.memory.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;

import java.io.Reader;

public class ContentAnalyzer extends Analyzer {

    private final int shingleSize;
    private final boolean outputUnigrams;

    public ContentAnalyzer(int shingleSize, boolean outputUnigrams) {
        super(GLOBAL_REUSE_STRATEGY);

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
