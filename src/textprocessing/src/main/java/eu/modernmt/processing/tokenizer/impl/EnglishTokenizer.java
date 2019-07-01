package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.abbr.AbbreviationAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.EnglishTokenAnnotator;

import java.io.Reader;

public class EnglishTokenizer extends BaseTokenizer {

    public EnglishTokenizer() {
        super.annotators.add(AbbreviationAnnotator.getInstance(Language.ENGLISH, true));
        super.annotators.add(new EnglishTokenAnnotator((Reader) null));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
