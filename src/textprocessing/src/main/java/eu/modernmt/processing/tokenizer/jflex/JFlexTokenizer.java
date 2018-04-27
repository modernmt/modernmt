package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.tokenizer.jflex.annotators.*;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
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

	The language is a Language object, obtained as Language.LANGUAGE_NAME
	The Annotator class is taken from JFlex library as AnnotatorLanguageName.class.
	In JFlex, all specific annotators extend a common interface Annotator.*/
    private static final Map<Language, Class<? extends JFlexTokenAnnotator>> ANNOTATORS = new HashMap<>();

    static {
        ANNOTATORS.put(Language.CATALAN, CatalanTokenAnnotator.class);
        ANNOTATORS.put(Language.CZECH, CzechTokenAnnotator.class);
        ANNOTATORS.put(Language.GERMAN, GermanTokenAnnotator.class);
        ANNOTATORS.put(Language.GREEK, GreekTokenAnnotator.class);
        ANNOTATORS.put(Language.ENGLISH, EnglishTokenAnnotator.class);
        ANNOTATORS.put(Language.SPANISH, SpanishTokenAnnotator.class);
        ANNOTATORS.put(Language.FINNISH, FinnishTokenAnnotator.class);
        ANNOTATORS.put(Language.FRENCH, FrenchTokenAnnotator.class);
        ANNOTATORS.put(Language.HUNGARIAN, HungarianTokenAnnotator.class);
        ANNOTATORS.put(Language.ICELANDIC, IcelandicTokenAnnotator.class);
        ANNOTATORS.put(Language.ITALIAN, ItalianTokenAnnotator.class);
        ANNOTATORS.put(Language.LATVIAN, LatvianTokenAnnotator.class);
        ANNOTATORS.put(Language.DUTCH, DutchTokenAnnotator.class);
        ANNOTATORS.put(Language.POLISH, PolishTokenAnnotator.class);
        ANNOTATORS.put(Language.PORTUGUESE, PortugueseTokenAnnotator.class);
        ANNOTATORS.put(Language.ROMANIAN, RomanianTokenAnnotator.class);
        ANNOTATORS.put(Language.RUSSIAN, RussianTokenAnnotator.class);
        ANNOTATORS.put(Language.SLOVAK, SlovakTokenAnnotator.class);
        ANNOTATORS.put(Language.SLOVENE, SloveneTokenAnnotator.class);
        ANNOTATORS.put(Language.SWEDISH, SwedishTokenAnnotator.class);
        ANNOTATORS.put(Language.TAMIL, TamilTokenAnnotator.class);
    }

    /*among all annotators for all Language supported by JFlex,
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
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public JFlexTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        this(sourceLanguage, targetLanguage, loadAnnotator(sourceLanguage));
    }

    private static JFlexTokenAnnotator loadAnnotator(Language sourceLanguage) {
        Class<? extends JFlexTokenAnnotator> annotatorClass = ANNOTATORS.get(sourceLanguage);

        if (annotatorClass == null) {
            return new StandardTokenAnnotator((Reader) null);
        } else {
            try {
                return annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + annotatorClass.getName(), e);
            }
        }
    }

    protected JFlexTokenizer(Language sourceLanguage, Language targetLanguage, JFlexTokenAnnotator annotator) {
        super(sourceLanguage, targetLanguage);
        this.annotator = annotator;
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
     * that is passed to the TokenizerUtils static object
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
        TokensAnnotatedString astring = wrap(builder);

        annotator.yyreset(astring.getReader());

        int type;
        while ((type = next(annotator)) != JFlexTokenAnnotator.YYEOF) {
            annotator.annotate(astring, type);
        }

        return astring.compile(builder);
    }

    protected TokensAnnotatedString wrap(SentenceBuilder builder) {
        return new TokensAnnotatedString(builder.toString(), false);
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
