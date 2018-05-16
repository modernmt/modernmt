package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.DutchTokenAnnotator;

import java.io.Reader;

public class DutchTokenizer extends BaseTokenizer {

    public DutchTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        super.annotators.add(new DutchTokenAnnotator((Reader) null));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
