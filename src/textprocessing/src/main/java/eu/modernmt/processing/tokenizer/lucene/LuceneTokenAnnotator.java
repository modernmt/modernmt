package eu.modernmt.processing.tokenizer.lucene;

import eu.modernmt.lang.Language2;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;
import eu.modernmt.processing.tokenizer.lucene.analyzers.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LuceneTokenAnnotator implements BaseTokenizer.Annotator {

    private static final Map<Language2, Class<? extends Analyzer>> ANALYZERS = new HashMap<>();

    static {
        ANALYZERS.put(Language2.ARABIC, ArabicAnalyzer.class);
        ANALYZERS.put(Language2.GERMAN, GermanAnalyzer.class);
        ANALYZERS.put(Language2.PERSIAN, PersianAnalyzer.class);
        ANALYZERS.put(Language2.HINDI, HindiAnalyzer.class);
        ANALYZERS.put(Language2.THAI, ThaiAnalyzer.class);
        ANALYZERS.put(Language2.HEBREW, HebrewAnalyzer.class);

        // Standard analyzer
        ANALYZERS.put(Language2.BULGARIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.BRAZILIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.CATALAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.CZECH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.DANISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.GREEK, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.ENGLISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.SPANISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.BASQUE, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.FINNISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.FRENCH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.IRISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.GALICIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.HUNGARIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.ARMENIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.INDONESIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.ITALIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.LATVIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.DUTCH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.NORWEGIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.PORTUGUESE, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.ROMANIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.RUSSIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.SWEDISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language2.TURKISH, LiteStandardAnalyzer.class);
    }

    private final Analyzer analyzer;

    public static LuceneTokenAnnotator forLanguage(Language2 language) throws UnsupportedLanguageException {
        Class<? extends Analyzer> analyzerClass = ANALYZERS.get(language);
        if (analyzerClass == null)
            throw new UnsupportedLanguageException(language);

        try {
            return new LuceneTokenAnnotator(analyzerClass.newInstance());
        } catch (IllegalAccessException | InstantiationException e) {
            throw new Error("Error during class instantiation: " + analyzerClass.getName(), e);
        }
    }

    private LuceneTokenAnnotator(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public void annotate(TokenizedString string) throws ProcessingException {
        char[] chars = string.toString().toCharArray();

        TokenStream stream = null;

        try {
            stream = analyzer.tokenStream("none", string.toString());
            stream.reset();

            OffsetAttribute offsetAttribute = stream.getAttribute(OffsetAttribute.class);

            int maxOffset = 0;
            while (stream.incrementToken()) {
                int startOffset = offsetAttribute.startOffset();
                int endOffset = offsetAttribute.endOffset();

                startOffset = Math.max(startOffset, maxOffset);
                endOffset = Math.max(endOffset, maxOffset);

                if (startOffset >= chars.length || endOffset >= chars.length)
                    break;

                if (startOffset > maxOffset && maxOffset < chars.length)
                    annotate(string, chars, maxOffset, startOffset - maxOffset);

                if (endOffset > startOffset)
                    annotate(string, chars, startOffset, endOffset - startOffset);

                maxOffset = endOffset;

                // Skip whitespaces
                while (maxOffset < chars.length && Character.isWhitespace(chars[maxOffset]))
                    maxOffset++;
            }

            stream.close();

            if (maxOffset < chars.length)
                annotate(string, chars, maxOffset, chars.length - maxOffset);
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

    private static void annotate(TokenizedString string, char[] chars, int offset, int length) {
        int beginIndex = -1;

        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(chars[offset + i])) {
                if (beginIndex >= 0) {
                    string.setWord(beginIndex + offset, i + offset);
                    beginIndex = -1;
                }
            } else {
                if (beginIndex < 0)
                    beginIndex = i;
            }
        }

        if (beginIndex >= 0)
            string.setWord(beginIndex + offset, length + offset);
    }

}
