package eu.modernmt.processing.tokenizer.lucene;


import eu.modernmt.model.Languages;
import eu.modernmt.model.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
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
 * Updated by andrearossi on 01/03/2017
 * <p>
 * A LuceneTokenizer is an object that performs word tokenization of a string
 * based on the Lucene tokenization and analysis library.
 * <p>
 * It has knowledge of all the tokenizer classes (that Lucene calls analyzers)
 * that Lucene can employ, one for each source language that Lucene supports.
 * The languages that the Lucene library supports are:
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

	The language is a Locale object, obtained as Languages.LANGUAGE_NAME
	The Analyzer is taken from Lucene library as AnalyzerClassName.class.
	In Lucene, all specific analyzers extend a common interface Analyzer.*/
    private static final Map<Locale, Class<? extends Analyzer>> ANALYZERS = new HashMap<>();

    static {
        ANALYZERS.put(Languages.ARABIC, ArabicAnalyzer.class);
        ANALYZERS.put(Languages.GERMAN, GermanAnalyzer.class);
        ANALYZERS.put(Languages.PERSIAN, PersianAnalyzer.class);
        ANALYZERS.put(Languages.HINDI, HindiAnalyzer.class);
        ANALYZERS.put(Languages.THAI, ThaiAnalyzer.class);
        ANALYZERS.put(Languages.HEBREW, HebrewAnalyzer.class);

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

    /*among all analyzers for all languages supported by Lucene,
    * this is the analyzer for the source language
    * (the language of the SentenceBuilder string to edit)*/
    private final Analyzer analyzer;

    /**
     * This constructor initializes the LuceneTokenizer
     * by setting the source and target language to handle,
     * and by choosing and trying to instantiate the specific Lucene analyzer
     * that suits the source language of the string to translate.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public LuceneTokenizer(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
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

    /**
     * Method that performs tokenization of the string under analysis
     * by skipping whitespace characters sequences.
     * <p>
     * It splits the string to tokenize in correspondence of "\\s+" occurrences,
     * (that are just sequences of one or more whitespace characters)
     * filters out the resulting tokens that are empty, and
     * adds the relevant tokens to the current tokens list.
     *
     * @param string the string to tokenize
     * @param tokens the current list or non-whitespace tokens
     */
    private static void whitespaceTokenize(String string, ArrayList<String> tokens) {
        for (String token : string.trim().split("\\s+")) {
            token = token.trim();

            if (!token.isEmpty())
                tokens.add(token);
        }
    }

    /**
     * This method uses the Analyzer object for the current source language
     * to perform word tokenization of the current string in the SentenceBuilder.
     * <p>
     * It extracts the current string to process from the builder
     * and scans it by employing a tokenstream.
     * For each substring extracted by the tokenstream,
     * it performs cleaning by removing its internal whitespace sequences.
     * <p>
     * In the end it passes the token Strings arrayList to the
     * TokenizerOutputTransformer static object
     * so that it can transform each token String into an actual WORD Token.*
     *
     * @param builder  the SentenceBuilder that holds the current string to tokenize
     * @param metadata additional information on the current pipe
     *                 (not used in this specific operation)
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the execution of the call() method
     * @throws ProcessingException
     */
    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) throws ProcessingException {
        char[] chars = builder.toCharArray();

        TokenStream stream = null;

        try {
            stream = analyzer.tokenStream("none", builder.toString());
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

            /*handles the last token, if it has been skipped by the tokenStream*/
            if (maxOffset < chars.length) {
                String skippedToken = new String(chars, maxOffset, chars.length - maxOffset).trim();

                for (String token : skippedToken.split("\\s+")) {
                    token = token.trim();

                    if (!token.isEmpty())
                        tokens.add(token);
                }
            }

            return TokenizerOutputTransformer.transform(builder, tokens);
        } catch (IOException e) {
            throw new ProcessingException(e.getMessage(), e);

            /*close the stream anyway*/
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
