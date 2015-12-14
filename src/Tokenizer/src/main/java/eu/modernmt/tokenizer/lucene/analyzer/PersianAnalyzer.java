package eu.modernmt.tokenizer.lucene.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ar.ArabicNormalizationFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.fa.PersianCharFilter;
import org.apache.lucene.analysis.fa.PersianNormalizationFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.Reader;

/**
 * Created by davide on 12/11/15.
 */
public class PersianAnalyzer extends Analyzer {

    /**
     * Creates
     * {@link TokenStreamComponents}
     * used to tokenize all the text in the provided {@link Reader}.
     *
     * @return {@link TokenStreamComponents}
     * built from a {@link StandardTokenizer} filtered with
     * {@link LowerCaseFilter}, {@link ArabicNormalizationFilter},
     * {@link PersianNormalizationFilter} and Persian Stop words
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName,
                                                     Reader reader) {
        final Tokenizer source = new StandardTokenizer(reader);
        TokenStream result = new ArabicNormalizationFilter(source);
        result = new PersianNormalizationFilter(result);
        return new TokenStreamComponents(source, result);
    }

    /**
     * Wraps the Reader with {@link PersianCharFilter}
     */
    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        return new PersianCharFilter(reader);
    }
}
