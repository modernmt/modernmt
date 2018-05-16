package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.languagetool.LanguageToolTokenAnnotator;

import java.io.Reader;

public class CentralKhmerTokenizer extends BaseTokenizer {

    public CentralKhmerTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        super.annotators.add(LanguageToolTokenAnnotator.forLanguage(sourceLanguage));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
