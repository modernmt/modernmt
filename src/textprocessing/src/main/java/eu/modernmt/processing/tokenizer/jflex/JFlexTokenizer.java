package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.model.Languages;
import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import eu.modernmt.processing.tokenizer.jflex.annotators.*;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 29/01/16.
 * Updated by andrearossi on 01/03/2017
 * <p>
 * A JFlexTokenizer is an object that performs word tokenization of a string
 * based on the JFlexTokenizer tokenization and analysis library.
 * <p>
 * It has knowledge of all the JFlex tokenization classes
 * (that JFlex implements as Annotators),
 * one for each source language that JFlex supports:
 * Catalan, Czech, German, Greek, ENglish, Spanish, Finnish, French, Hungarian,
 * Icelandic, Italian, Latvian, Dutch, Polish, Portuguese, Romanian, Russian,
 * Slovak, Slovene, Swedish, Tamil.
 */
public class JFlexTokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /*For each language that the JFlex library supports, this map stores a couple
        <language -> Class of the JFlex annotator for that language>

	The language is a Locale object, obtained as Languages.LANGUAGE_NAME
	The Annotator class is taken from JFlex library as AnnotatorLanguageName.class.
	In JFlex, all specific annotators extend a common interface Annotator.*/
    private static final Map<Locale, Class<? extends JFlexTokenAnnotator>> ANNOTATORS = new HashMap<>();

    static {
        ANNOTATORS.put(Languages.CATALAN, CatalanTokenAnnotator.class);
        ANNOTATORS.put(Languages.CZECH, CzechTokenAnnotator.class);
        ANNOTATORS.put(Languages.GERMAN, GermanTokenAnnotator.class);
        ANNOTATORS.put(Languages.GREEK, GreekTokenAnnotator.class);
        ANNOTATORS.put(Languages.ENGLISH, EnglishTokenAnnotator.class);
        ANNOTATORS.put(Languages.SPANISH, SpanishTokenAnnotator.class);
        ANNOTATORS.put(Languages.FINNISH, FinnishTokenAnnotator.class);
        ANNOTATORS.put(Languages.FRENCH, FrenchTokenAnnotator.class);
        ANNOTATORS.put(Languages.HUNGARIAN, HungarianTokenAnnotator.class);
        ANNOTATORS.put(Languages.ICELANDIC, IcelandicTokenAnnotator.class);
        ANNOTATORS.put(Languages.ITALIAN, ItalianTokenAnnotator.class);
        ANNOTATORS.put(Languages.LATVIAN, LatvianTokenAnnotator.class);
        ANNOTATORS.put(Languages.DUTCH, DutchTokenAnnotator.class);
        ANNOTATORS.put(Languages.POLISH, PolishTokenAnnotator.class);
        ANNOTATORS.put(Languages.PORTUGUESE, PortugueseTokenAnnotator.class);
        ANNOTATORS.put(Languages.ROMANIAN, RomanianTokenAnnotator.class);
        ANNOTATORS.put(Languages.RUSSIAN, RussianTokenAnnotator.class);
        ANNOTATORS.put(Languages.SLOVAK, SlovakTokenAnnotator.class);
        ANNOTATORS.put(Languages.SLOVENE, SloveneTokenAnnotator.class);
        ANNOTATORS.put(Languages.SWEDISH, SwedishTokenAnnotator.class);
        ANNOTATORS.put(Languages.TAMIL, TamilTokenAnnotator.class);
    }

    /*among all annotators for all languages supported by JFlex,
     * this is the specific annotator for the source language
     * (the language of the SentenceBuilder string to edit)*/
    private final JFlexTokenAnnotator annotator;

    /**
     * This constructor initializes che JFlexTokenizer
     * by setting the source and target language to handle,
     * and by choosing and trying to instantiate the specific JFLex annotator
     * that suits the source language of the string to translate.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws LanguageNotSupportedException the requested language is not supported by this software
     */
    public JFlexTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        Class<? extends JFlexTokenAnnotator> annotatorClass = ANNOTATORS.get(sourceLanguage);

        if (annotatorClass == null) {
            this.annotator = new StandardTokenAnnotator((Reader) null);
        } else {
            try {
                this.annotator = annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + annotatorClass.getName(), e);
            }
        }
    }

    /**
     * This method uses the Annotator object for the current source language
     * to perform word tokenization of the current string in the SentenceBuilder.
     * <p>
     * It extracts the string from tne StringBuilder, and uses it to
     * build a TokensAnnotatedString.
     * By the interaction between annotator and annotatedString,
     * the indexes of start and end of each token are found
     * and stored in the annotatedString.
     * <p>
     * In the end, such indexes are used to build an array of token strings,
     * that is passed to the TokenizerOutputTransformer static object
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
        TokensAnnotatedString astring = new TokensAnnotatedString(builder.toString());

        annotator.yyreset(astring.getReader());

        int type;
        while ((type = next(annotator)) != JFlexTokenAnnotator.YYEOF) {
            annotator.annotate(astring, type);
        }

        return TokenizerOutputTransformer.transform(builder, astring.toTokenArray());
    }


    /**
     * Method that, given an annotator, returns the next token index
     * in the string the annotator is working on.
     *
     * @param annotator the JFlexAnnotator for the string under analysis
     * @return the index of the next token
     * @throws ProcessingException
     */
    private static int next(JFlexTokenAnnotator annotator) throws ProcessingException {
        try {
            return annotator.next();
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

}
