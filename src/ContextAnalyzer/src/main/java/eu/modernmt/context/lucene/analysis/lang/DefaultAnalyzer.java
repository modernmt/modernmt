package eu.modernmt.context.lucene.analysis.lang;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;

import java.io.IOException;
import java.io.Reader;

public class DefaultAnalyzer extends LanguageAnalyzer {

    public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;
    private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

    protected DefaultAnalyzer(AnalyzerConfig config, CharArraySet defaultStopWordsSet) {
        super(config, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final StandardTokenizer source = new StandardTokenizer();

        TokenStream tok = new StandardFilter(source);

        if (config.toLowerCase)
            tok = new LowerCaseFilter(tok);
        if (config.filterStopWords)
            tok = new StopFilter(tok, stopwords);

        if (config.enableStemming) {
            source.setMaxTokenLength(maxTokenLength);
            return new TokenStreamComponents(source, tok) {

                @Override
                protected void setReader(final Reader reader) throws IOException {
                    ((StandardTokenizer) source).setMaxTokenLength(DefaultAnalyzer.this.maxTokenLength);
                    super.setReader(reader);
                }

            };
        } else {
            return new TokenStreamComponents(source, tok);
        }
    }

}
