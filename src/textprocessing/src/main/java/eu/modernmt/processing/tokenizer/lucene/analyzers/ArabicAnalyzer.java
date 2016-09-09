package eu.modernmt.processing.tokenizer.lucene.analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ar.ArabicNormalizationFilter;
import org.apache.lucene.analysis.ar.ArabicStemFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.Reader;

/**
 * Created by davide on 12/11/15.
 */
public class ArabicAnalyzer extends Analyzer {

    /**
     * Creates
     * {@link TokenStreamComponents}
     * used to tokenize all the text in the provided {@link Reader}.
     *
     * @return {@link TokenStreamComponents}
     * built from an {@link StandardTokenizer} filtered with
     * {@link LowerCaseFilter}, {@link StopFilter},
     * {@link ArabicNormalizationFilter}, {@link SetKeywordMarkerFilter}
     * if a stem exclusion set is provided and {@link ArabicStemFilter}.
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName,
                                                     Reader reader) {
        final Tokenizer source = new StandardTokenizer(reader);
        TokenStream result = new ArabicNormalizationFilter(source);
        return new TokenStreamComponents(source, result);
    }

}
