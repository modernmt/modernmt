package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language2;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.abbr.AbbreviationAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;

import java.io.Reader;

public class FinnishTokenizer extends BaseTokenizer {

    public FinnishTokenizer() {
        super.annotators.add(AbbreviationAnnotator.getInstance(Language2.FINNISH));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
