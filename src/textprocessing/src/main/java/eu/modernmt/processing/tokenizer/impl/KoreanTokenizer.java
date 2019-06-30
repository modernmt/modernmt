package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;

import java.io.Reader;

public class KoreanTokenizer extends BaseTokenizer {

    public KoreanTokenizer() {
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
