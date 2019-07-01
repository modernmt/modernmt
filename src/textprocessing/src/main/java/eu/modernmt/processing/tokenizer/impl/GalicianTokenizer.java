package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.languagetool.LanguageToolTokenAnnotator;

import java.io.Reader;

public class GalicianTokenizer extends BaseTokenizer {

    public GalicianTokenizer() {
        super.annotators.add(LanguageToolTokenAnnotator.forLanguage(Language.GALICIAN));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
