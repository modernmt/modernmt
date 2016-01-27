package eu.modernmt.processing.tokenizer.lucene.analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.hi.HindiNormalizationFilter;
import org.apache.lucene.analysis.hi.HindiStemFilter;
import org.apache.lucene.analysis.in.IndicNormalizationFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.Reader;

/**
 * Created by davide on 12/11/15.
 */
public class HindiAnalyzer extends Analyzer {

    /**
     * Creates
     * {@link TokenStreamComponents}
     * used to tokenize all the text in the provided {@link Reader}.
     *
     * @return {@link TokenStreamComponents}
     * built from a {@link StandardTokenizer} filtered with
     * {@link LowerCaseFilter}, {@link IndicNormalizationFilter},
     * {@link HindiNormalizationFilter}, {@link SetKeywordMarkerFilter}
     * if a stem exclusion set is provided, {@link HindiStemFilter}, and
     * Hindi Stop words
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName,
                                                     Reader reader) {
        final Tokenizer source = new StandardTokenizer(reader);

        TokenStream result = new IndicNormalizationFilter(source);
        result = new HindiNormalizationFilter(result);
        return new TokenStreamComponents(source, result);
    }

}
