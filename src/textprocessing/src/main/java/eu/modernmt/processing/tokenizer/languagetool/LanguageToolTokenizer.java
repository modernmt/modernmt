package eu.modernmt.processing.tokenizer.languagetool;

import eu.modernmt.lang.Languages;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.tokenizer.TokenizerUtils;
import org.languagetool.language.tokenizers.TagalogWordTokenizer;
import org.languagetool.tokenizers.br.BretonWordTokenizer;
import org.languagetool.tokenizers.eo.EsperantoWordTokenizer;
import org.languagetool.tokenizers.gl.GalicianWordTokenizer;
import org.languagetool.tokenizers.km.KhmerWordTokenizer;
import org.languagetool.tokenizers.ml.MalayalamWordTokenizer;
import org.languagetool.tokenizers.uk.UkrainianWordTokenizer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 12/11/15.
 * Updated by andrearossi on 01/03/2017
 * <p>
 * A LanguageToolTokenizer is an object that performs word tokenization of a string
 * based on the languageTool tokenization library.
 * <p>
 * It has knowledge of all the tokenizer classes that languageTool can employ,
 * one for each source language that languageTool supports.
 * The languages that the languageTool library supports are:
 * Breton, Esperanto, Galician, Khmer, Malayalam, Ukrainian, Tagalog.
 */
public class LanguageToolTokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {
    /*For each language that the languageTool library supports,
      this map stores a couple
       <language -> Class of the languageTool tokenizer for that language>

        The language is a Locale object, obtained as Languages.LANGUAGE_NAME
        The Tokenizer is taken from languageTool library as LanguageNameTokenizer.class.
        In languageTool, all specific Tokenizers extend a common interface Tokenizer.*/
    private static final Map<Locale, Class<? extends org.languagetool.tokenizers.Tokenizer>> TOKENIZERS = new HashMap<>();

    static {
        TOKENIZERS.put(Languages.BRETON, BretonWordTokenizer.class);
        TOKENIZERS.put(Languages.ESPERANTO, EsperantoWordTokenizer.class);
        TOKENIZERS.put(Languages.GALICIAN, GalicianWordTokenizer.class);
        TOKENIZERS.put(Languages.KHMER, KhmerWordTokenizer.class);
        TOKENIZERS.put(Languages.MALAYALAM, MalayalamWordTokenizer.class);
        TOKENIZERS.put(Languages.UKRAINIAN, UkrainianWordTokenizer.class);
        TOKENIZERS.put(Languages.TAGALOG, TagalogWordTokenizer.class);

        /* Excluded tokenizers */
//        TOKENIZERS.put(Languages.CATALAN, CatalanWordTokenizer.class);
//        TOKENIZERS.put(Languages.GREEK, GreekWordTokenizer.class);
//        TOKENIZERS.put(Languages.ENGLISH, EnglishWordTokenizer.class);
//        TOKENIZERS.put(Languages.SPANISH, SpanishWordTokenizer.class);
//        TOKENIZERS.put(Languages.JAPANESE, JapaneseWordTokenizer.class);
//        TOKENIZERS.put(Languages.DUTCH, DutchWordTokenizer.class);
//        TOKENIZERS.put(Languages.POLISH, PolishWordTokenizer.class);
//        TOKENIZERS.put(Languages.ROMANIAN, RomanianWordTokenizer.class);
    }

    /*among all tokenizers for all languages supported by languageTool,
     * this is the tokenizer for the source language
     * (the language of the SentenceBuilder string to edit)*/
    private org.languagetool.tokenizers.Tokenizer tokenizer;

    /**
     * This constructor initializes che LanguageToolTokenizer
     * by setting the source and target language to handle,
     * and by choosing and trying to instantiate
     * the specific languageTool Tokenizer
     * that suits the source language of the string to translate.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public LanguageToolTokenizer(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        Class<? extends org.languagetool.tokenizers.Tokenizer> tokenizerClass = TOKENIZERS.get(sourceLanguage);
        if (tokenizerClass == null)
            throw new UnsupportedLanguageException(sourceLanguage);

        try {
            this.tokenizer = tokenizerClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new Error("Error during class instantiation: " + tokenizerClass.getName(), e);
        }
    }

    /**
     * This method uses the Tokenizer object for the current source language
     * to perform word tokenization of the current string in the SentenceBuilder.
     * <p>
     * It extracts the current string to process from the builder
     * and passes it to the Tokenizer, thus obtaining a list of tokens,
     * each of which is in the form of a simple String.
     * The tokens undergo a trimming step, and those that are now empty
     * are filtered out.
     * <p>
     * An arraylist containing all the relevant token Strings
     * is finally passed to the TokenizerUtils static object,
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
        List<String> tokens = tokenizer.tokenize(builder.toString());
        ArrayList<String> result = new ArrayList<>(tokens.size());

        result.addAll(tokens.stream().filter(token -> !token.trim().isEmpty()).collect(Collectors.toList()));

        return TokenizerUtils.transform(builder, result);
    }

}
