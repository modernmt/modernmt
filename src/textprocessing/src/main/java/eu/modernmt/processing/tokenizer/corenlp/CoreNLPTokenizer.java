package eu.modernmt.processing.tokenizer.corenlp;

import edu.stanford.nlp.international.arabic.process.ArabicTokenizer;
import edu.stanford.nlp.international.french.process.FrenchTokenizer;
import edu.stanford.nlp.international.spanish.process.SpanishTokenizer;
import edu.stanford.nlp.ling.HasOffset;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 11/11/15.
 * Updated by andrearossi on 01/03/2017
 * <p>
 * A CoreNLPTokenizer is an object that performs word tokenization of a string
 * based on the CoreNLP tokenization and analysis library.
 * <p>
 * It has knowledge of all the tokenizer factory classes that CoreNLP employs,
 * one for each source language that CoreNLP supports.
 * The languages that the CoreNLP library supports are:
 * English, Arabic, French, Spanish.
 */
public class CoreNLPTokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /*For each language that the CoreNLP library supports, this map stores a couple
       <language -> Reference to CoreNLP Factory of tokenizers for that language>

   * The language is a Language object, obtained as Languages.LANGUAGE_NAME
   * The Factory reference is obtained as SPECIFIC_LANGUAGE_Tokenizer.factory()*/
    private static final Map<Language, TokenizerFactory<?>> FACTORIES = new HashMap<>();

    static {
        FACTORIES.put(Language.ENGLISH, PTBTokenizer.factory());
        FACTORIES.put(Language.ARABIC, ArabicTokenizer.factory());
        FACTORIES.put(Language.FRENCH, FrenchTokenizer.factory());
        FACTORIES.put(Language.SPANISH, SpanishTokenizer.factory());
    }

    /*among all factories for all languages tokenizers in CoreNLP,
     * this is the analyzer for the tokenizer of the source language
     * (the language of the SentenceBuilder string to edit)*/
    private final TokenizerFactory<?> factory;

    /**
     * This constructor initializes the CoreNLPTokenizer
     * by setting the source and target language to handle,
     * and by choosing the specific CoreNLP tokenizer factory
     * that suits the source language of the string to translate.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public CoreNLPTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        this.factory = FACTORIES.get(sourceLanguage);
        if (this.factory == null)
            throw new UnsupportedLanguageException(sourceLanguage);

        /*sets special options if source language is English*/
        if (Language.ENGLISH.getLanguage().equals(sourceLanguage.getLanguage()))
            this.factory.setOptions("ptb3Escaping=false,asciiQuotes=true,normalizeSpace=false");
    }

    /**
     * This method uses the factory object stored in the CoreNLPTokenizer
     * to create a specific CoreNLP (stanford) Library Tokenizer object
     * for the current source language and the string under analysis,
     * which is extracted as the current string of the SentenceBuilder.
     * <p>
     * The CoreNLP library tokenizer just acts as an iterator
     * over the tokens extracted from the string under analysis.
     * The strings contained in relevant (non empty) tokens are added
     * to one common String arrayList.
     * <p>
     * In the end, the token Strings arrayList is passed to the
     * TokenizerUtils static object
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
        Reader reader = new StringReader(builder.toString());
        edu.stanford.nlp.process.Tokenizer<?> tokenizer;
        synchronized (this) {
            tokenizer = this.factory.getTokenizer(reader);
        }

        SentenceBuilder.Editor editor = builder.edit();

        while (tokenizer.hasNext()) {
            Object token = tokenizer.next();

            if (token instanceof HasOffset) {
                HasOffset hasOffset = (HasOffset) token;
                int begin = hasOffset.beginPosition();
                int end = hasOffset.endPosition();

                editor.setWord(begin, end - begin, null);
            }
        }

        return editor.commit();
    }

}
