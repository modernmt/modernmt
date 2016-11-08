package eu.modernmt.processing.tokenizer.lucene;


import eu.modernmt.model.Languages;
import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.XMLEditableString;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import eu.modernmt.processing.tokenizer.lucene.analyzers.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 13/11/15.
 */
public class LuceneTokenizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    private static final Map<Locale, Class<? extends Analyzer>> ANALYZERS = new HashMap<>();

    static {
        ANALYZERS.put(Languages.ARABIC, ArabicAnalyzer.class);
        ANALYZERS.put(Languages.GERMAN, GermanAnalyzer.class);
        ANALYZERS.put(Languages.PERSIAN, PersianAnalyzer.class);
        ANALYZERS.put(Languages.HINDI, HindiAnalyzer.class);
        ANALYZERS.put(Languages.THAI, ThaiAnalyzer.class);
        ANALYZERS.put(Languages.HEBREW, HebrewAnalyzer.class);
        ANALYZERS.put(Languages.CHINESE, PaodingAnalyzer.class);

        // Standard analyzer
        ANALYZERS.put(Languages.BULGARIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.BRAZILIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.CATALAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.CZECH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.DANISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.GREEK, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.ENGLISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.SPANISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.BASQUE, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.FINNISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.FRENCH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.IRISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.GALICIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.HUNGARIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.ARMENIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.INDONESIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.ITALIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.LATVIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.DUTCH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.NORWEGIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.PORTUGUESE, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.ROMANIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.RUSSIAN, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.SWEDISH, LiteStandardAnalyzer.class);
        ANALYZERS.put(Languages.TURKISH, LiteStandardAnalyzer.class);
    }

    private final Analyzer analyzer;

    public LuceneTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        Class<? extends Analyzer> analyzerClass = ANALYZERS.get(sourceLanguage);
        if (analyzerClass == null)
            throw new LanguageNotSupportedException(sourceLanguage);

        try {
            this.analyzer = analyzerClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new Error("Error during class instantiation: " + analyzerClass.getName(), e);
        }
    }

    private static void whitespaceTokenize(String string, ArrayList<String> tokens) {
        for (String token : string.trim().split("\\s+")) {
            token = token.trim();

            if (!token.isEmpty())
                tokens.add(token);
        }
    }

    @Override
    public XMLEditableString call(XMLEditableString text, Map<String, Object> metadata) throws ProcessingException {
        char[] chars = text.toCharArray();

        TokenStream stream = null;

        try {
            stream = analyzer.tokenStream("none", text.toString());
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

                if (startOffset > maxOffset && maxOffset < chars.length)
                    whitespaceTokenize(new String(chars, maxOffset, startOffset - maxOffset), tokens);

                if (endOffset > startOffset)
                    whitespaceTokenize(new String(chars, startOffset, endOffset - startOffset), tokens);

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

            return TokenizerOutputTransformer.transform(text, tokens);
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
}
