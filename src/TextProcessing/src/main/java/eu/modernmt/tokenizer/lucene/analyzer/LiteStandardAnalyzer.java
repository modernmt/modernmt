package eu.modernmt.tokenizer.lucene.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 12/11/15.
 */
public class LiteStandardAnalyzer extends Analyzer {

    /**
     * Default maximum allowed token length
     */
    public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

    private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

    /**
     * Set maximum allowed token length.  If a token is seen
     * that exceeds this length then it is discarded.  This
     * setting only takes effect the next time tokenStream or
     * tokenStream is called.
     */
    public void setMaxTokenLength(int length) {
        maxTokenLength = length;
    }

    /**
     * @see #setMaxTokenLength
     */
    public int getMaxTokenLength() {
        return maxTokenLength;
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName, final Reader reader) {
        final StandardTokenizer src = new StandardTokenizer(reader);
        src.setMaxTokenLength(maxTokenLength);

        return new TokenStreamComponents(src) {
            @Override
            protected void setReader(final Reader reader) throws IOException {
                src.setMaxTokenLength(LiteStandardAnalyzer.this.maxTokenLength);
                super.setReader(reader);
            }
        };
    }
}
