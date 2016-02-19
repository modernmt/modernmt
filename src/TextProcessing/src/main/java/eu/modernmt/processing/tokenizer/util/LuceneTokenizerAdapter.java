package eu.modernmt.processing.tokenizer.util;

import eu.modernmt.processing.AnnotatedString;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.tokenizer.MultiInstanceTokenizer;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by davide on 26/01/16.
 */
public class LuceneTokenizerAdapter extends MultiInstanceTokenizer {

    protected static class AnalyzerTokenizerFactory implements TokenizerFactory {

        protected Class<? extends Analyzer> analyzerClass;

        public AnalyzerTokenizerFactory(Class<? extends Analyzer> analyzerClass) {
            this.analyzerClass = analyzerClass;
        }

        @Override
        public Tokenizer newInstance() {
            try {
                Analyzer analyzer = this.analyzerClass.newInstance();
                return new LuceneTokenizerImplementation(analyzer);
            } catch (IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + this.analyzerClass.getName(), e);
            }
        }
    }

    public LuceneTokenizerAdapter(Class<? extends Analyzer> analyzerClass) {
        this(new AnalyzerTokenizerFactory(analyzerClass));
    }

    public LuceneTokenizerAdapter(AnalyzerTokenizerFactory factory) {
        super(factory);
    }

    protected static class LuceneTokenizerImplementation implements Tokenizer {

        private Analyzer analyzer;

        public LuceneTokenizerImplementation(Analyzer analyzer) {
            this.analyzer = analyzer;
        }

        @Override
        public AnnotatedString call(String text) throws ProcessingException {
            char[] chars = text.toCharArray();

            TokenStream stream = null;

            try {
                stream = analyzer.tokenStream("none", text);
                stream.reset();

                OffsetAttribute offsetAttribute = stream.getAttribute(OffsetAttribute.class);

                ArrayList<String> tokens = new ArrayList<>();
                int maxOffset = 0;
                while (stream.incrementToken()) {
                    int startOffset = offsetAttribute.startOffset();
                    int endOffset = offsetAttribute.endOffset();

                    startOffset = Math.max(startOffset, maxOffset);
                    endOffset = Math.max(endOffset, maxOffset);

                    if (startOffset >= chars.length || endOffset >= chars.length)
                        break;

                    if (startOffset > maxOffset && maxOffset < chars.length) {
                        String skippedToken = new String(chars, maxOffset, startOffset - maxOffset).trim();

                        for (String token : skippedToken.split("\\s+")) {
                            token = token.trim();

                            if (!token.isEmpty())
                                tokens.add(token);
                        }
                    }

                    if (endOffset > startOffset)
                        tokens.add(new String(chars, startOffset, endOffset - startOffset));

                    maxOffset = endOffset;

                    // Skip whitespaces
                    while (maxOffset < chars.length && Character.isWhitespace(chars[maxOffset]))
                        maxOffset++;
                }

                stream.close();

                if (maxOffset < chars.length) {
                    String skippedToken = new String(chars, maxOffset, chars.length - maxOffset).trim();

                    for (String token : skippedToken.split("\\s+")) {
                        token = token.trim();

                        if (!token.isEmpty())
                            tokens.add(token);
                    }
                }

                return new AnnotatedString(text, TokenizerOutputTransformer.transform(text, tokens));
            } catch (IOException e) {
                throw new ProcessingException(e.getMessage(), e);
            } finally {
                if (stream != null)
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
            }
        }

        @Override
        public void close() {
        }
    }

}
