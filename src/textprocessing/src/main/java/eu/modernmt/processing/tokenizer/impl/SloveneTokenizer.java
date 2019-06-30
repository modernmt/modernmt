package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language2;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.abbr.AbbreviationAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;

import java.io.Reader;

public class SloveneTokenizer extends BaseTokenizer {

    public SloveneTokenizer() {
        super.annotators.add(AbbreviationAnnotator.getInstance(Language2.SLOVENE));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
