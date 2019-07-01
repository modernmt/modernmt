package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.abbr.AbbreviationAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;

import java.io.Reader;

public class HungarianTokenizer extends BaseTokenizer {

    public HungarianTokenizer() {
        super.annotators.add(AbbreviationAnnotator.getInstance(Language.HUNGARIAN));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
