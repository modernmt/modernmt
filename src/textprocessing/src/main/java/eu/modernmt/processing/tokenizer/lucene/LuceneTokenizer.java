package eu.modernmt.processing.tokenizer.lucene;


import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.tokenizer.lucene.analyzers.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 13/11/15.
 * Updated by andrearossi on 01/03/2017
 * <p>
 * A LuceneTokenizer is an object that performs word tokenization of a string
 * based on the Lucene tokenization and analysis library.
 * <p>
 * It has knowledge of all the tokenizer classes (that Lucene calls analyzers)
 * that Lucene can employ, one for each source language that Lucene supports.
 * The Language that the Lucene library supports are:
 * Arabic, German, Persian, Hindi, Thai, Hebrew, Chinese .
 * (for which Lucene has specific analyzer classes)
 * and Bulgarian, Brazilian, Catalan, Czech, Danish, Greek, English, Spanish, Basque
 * Finnish, French, Irish, Galician, Hungarian, Armenian, Indonesian, Italian,
 * Latvian, Dutch, Norwegian, Portuguese, Romanian, Russian, Swedish, Turkish
 * (that Lucene handles with a common, standard analyzer).
 */
public class LuceneTokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /*For each language that the Lucene library supports, this map stores a couple
    <language -> Class of the analyzer for that language in the Lucene library>

	The language is a Language object, obtained as Language.LANGUAGE_NAME
	The Analyzer is taken from Lucene library as AnalyzerClassName.class.
	In Lucene, all specific analyzers extend a common interface Analyzer.*/
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

    /*among all analyzers for all Language supported by Lucene,
     * this is the analyzer for the source language
     * (the language of the SentenceBuilder string to edit)*/
    private final Analyzer analyzer;

    public LuceneTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        Class<? extends Analyzer> analyzerClass = ANALYZERS.get(sourceLanguage);
        if (analyzerClass == null)
            throw new UnsupportedLanguageException(sourceLanguage);

        try {
            this.analyzer = analyzerClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new Error("Error during class instantiation: " + analyzerClass.getName(), e);
        }
    }

    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) throws ProcessingException {
        char[] chars = builder.toCharArray();

        TokenStream stream = null;

        try {
            stream = analyzer.tokenStream("none", builder.toString());
            stream.reset();

            OffsetAttribute offsetAttribute = stream.getAttribute(OffsetAttribute.class);

            SentenceBuilder.Editor editor = builder.edit();

            int maxOffset = 0;
            while (stream.incrementToken()) {
                int startOffset = offsetAttribute.startOffset();
                int endOffset = offsetAttribute.endOffset();

                startOffset = Math.max(startOffset, maxOffset);
                endOffset = Math.max(endOffset, maxOffset);

                if (startOffset >= chars.length || endOffset >= chars.length)
                    break;

                if (startOffset > maxOffset && maxOffset < chars.length)
                    annotate(editor, chars, maxOffset, startOffset - maxOffset);

                if (endOffset > startOffset)
                    annotate(editor, chars, startOffset, endOffset - startOffset);

                maxOffset = endOffset;

                // Skip whitespaces
                while (maxOffset < chars.length && Character.isWhitespace(chars[maxOffset]))
                    maxOffset++;
            }

            stream.close();

            if (maxOffset < chars.length)
                annotate(editor, chars, maxOffset, chars.length - maxOffset);

            return editor.commit();
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

    private static void annotate(SentenceBuilder.Editor editor, char[] chars, int offset, int length) {
        int beginIndex = -1;

        for (int i = 0; i < length; i++) {
            if (Character.isWhitespace(chars[offset + i])) {
                if (beginIndex >= 0) {
                    editor.setWord(beginIndex + offset, i - beginIndex, null);
                    beginIndex = -1;
                }
            } else {
                if (beginIndex < 0)
                    beginIndex = i;
            }
        }

        if (beginIndex >= 0)
            editor.setWord(beginIndex + offset, length - beginIndex, null);
    }
}
