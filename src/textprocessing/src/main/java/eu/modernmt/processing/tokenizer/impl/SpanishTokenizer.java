package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language2;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.abbr.AbbreviationAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;

import java.io.Reader;

public class SpanishTokenizer extends BaseTokenizer {

    public SpanishTokenizer() {
        super.annotators.add(AbbreviationAnnotator.getInstance(Language2.SPANISH));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
