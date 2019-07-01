package eu.modernmt.processing.tokenizer.lucene;

import eu.modernmt.lang.Language;
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

    private static final Map<Language, Class<? extends Analyzer>> ANALYZERS = new HashMap<>();

    static {
        ANALYZERS.put(Language.ARABIC, ArabicAnalyzer.class);
        ANALYZERS.put(Language.GERMAN, GermanAnalyzer.class);
        ANALYZERS.put(Language.PERSIAN, PersianAnalyzer.class);
        ANALYZERS.put(Language.HINDI, HindiAnalyzer.class);
        ANALYZERS.put(Language.THAI, ThaiAnalyzer.class);
        ANALYZERS.put(Language.HEBREW, HebrewAnalyzer.class);

        // Standard analyzer
        ANALYZERS.put(Language.BULGARIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.BRAZILIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.CATALAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.CZECH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.DANISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.GREEK, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.ENGLISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.SPANISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.BASQUE, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.FINNISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.FRENCH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.IRISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.GALICIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.HUNGARIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.ARMENIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.INDONESIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.ITALIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.LATVIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.DUTCH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.NORWEGIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.PORTUGUESE, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.ROMANIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.RUSSIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.SWEDISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Language.TURKISH, LiteStandardAnalyzer.class);
    }

    private final Analyzer analyzer;

    public static LuceneTokenAnnotator forLanguage(Language language) throws UnsupportedLanguageException {
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
