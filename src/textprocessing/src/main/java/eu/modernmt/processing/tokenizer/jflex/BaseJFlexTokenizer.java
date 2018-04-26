package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.jflex.annotators.StandardTokenAnnotator;

import java.io.Reader;

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
public class BaseJFlexTokenizer extends JFlexTokenizer {

    public BaseJFlexTokenizer(Language sourceLanguage, Language targetLanguage) {
        super(sourceLanguage, targetLanguage, new StandardTokenAnnotator((Reader) null));
    }

}
