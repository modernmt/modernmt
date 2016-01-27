package eu.modernmt.processing.tokenizer.lucene.analyzers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.th.ThaiTokenizer;

import java.io.Reader;

/**
 * Created by davide on 12/11/15.
 */
public class ThaiAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName,
                                                     Reader reader) {
        ThaiTokenizer source = new ThaiTokenizer(reader);
        TokenStream result = new StandardFilter(source);
        return new TokenStreamComponents(source, result);
    }
}
