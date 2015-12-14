package eu.modernmt.tokenizer.lucene;


import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import eu.modernmt.tokenizer.Languages;
import eu.modernmt.tokenizer.lucene.analyzer.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 13/11/15.
 */
public class LuceneTokenizer extends ITokenizer {

    public static final ITokenizerFactory ARABIC = new LuceneTokenizerFactory(ArabicAnalyzer.class);
    public static final ITokenizerFactory GERMAN = new LuceneTokenizerFactory(GermanAnalyzer.class);
    public static final ITokenizerFactory PERSIAN = new LuceneTokenizerFactory(PersianAnalyzer.class);
    public static final ITokenizerFactory HINDI = new LuceneTokenizerFactory(HindiAnalyzer.class);
    public static final ITokenizerFactory THAI = new LuceneTokenizerFactory(ThaiAnalyzer.class);

    public static final ITokenizerFactory BULGARIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory BRAZILIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory CATALAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory CZECH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory DANISH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory GREEK = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory ENGLISH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory SPANISH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory BASQUE = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory FINNISH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory FRENCH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory IRISH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory GALICIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory HUNGARIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory ARMENIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory INDONESIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory ITALIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory LATVIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory DUTCH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory NORWEGIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory PORTUGUESE = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory ROMANIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory RUSSIAN = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory SWEDISH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);
    public static final ITokenizerFactory TURKISH = new LuceneTokenizerFactory(LiteStandardAnalyzer.class);

    public static final Map<Locale, ITokenizerFactory> ALL = new HashMap<>();

    static {
        ALL.put(Languages.ARABIC, ARABIC);
        ALL.put(Languages.GERMAN, GERMAN);
        ALL.put(Languages.PERSIAN, PERSIAN);
        ALL.put(Languages.HINDI, HINDI);
        ALL.put(Languages.THAI, THAI);
        ALL.put(Languages.BULGARIAN, BULGARIAN);
        ALL.put(Languages.BRAZILIAN, BRAZILIAN);
        ALL.put(Languages.CATALAN, CATALAN);
        ALL.put(Languages.CZECH, CZECH);
        ALL.put(Languages.DANISH, DANISH);
        ALL.put(Languages.GREEK, GREEK);
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.SPANISH, SPANISH);
        ALL.put(Languages.BASQUE, BASQUE);
        ALL.put(Languages.FINNISH, FINNISH);
        ALL.put(Languages.FRENCH, FRENCH);
        ALL.put(Languages.IRISH, IRISH);
        ALL.put(Languages.GALICIAN, GALICIAN);
        ALL.put(Languages.HUNGARIAN, HUNGARIAN);
        ALL.put(Languages.ARMENIAN, ARMENIAN);
        ALL.put(Languages.INDONESIAN, INDONESIAN);
        ALL.put(Languages.ITALIAN, ITALIAN);
        ALL.put(Languages.LATVIAN, LATVIAN);
        ALL.put(Languages.DUTCH, DUTCH);
        ALL.put(Languages.NORWEGIAN, NORWEGIAN);
        ALL.put(Languages.PORTUGUESE, PORTUGUESE);
        ALL.put(Languages.ROMANIAN, ROMANIAN);
        ALL.put(Languages.RUSSIAN, RUSSIAN);
        ALL.put(Languages.SWEDISH, SWEDISH);
        ALL.put(Languages.TURKISH, TURKISH);
    }

    private Analyzer analyzer;

    public LuceneTokenizer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public String[] tokenize(String text) {
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

            return tokens.toArray(new String[tokens.size()]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (stream != null)
                try {
                    stream.close();
                } catch (IOException e) {
                }
        }
    }
}
