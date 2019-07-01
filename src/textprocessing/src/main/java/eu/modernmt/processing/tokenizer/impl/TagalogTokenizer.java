package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.languagetool.LanguageToolTokenAnnotator;

import java.io.Reader;

public class TagalogTokenizer extends BaseTokenizer {

    public TagalogTokenizer() {
        super.annotators.add(LanguageToolTokenAnnotator.forLanguage(Language.TAGALOG));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
