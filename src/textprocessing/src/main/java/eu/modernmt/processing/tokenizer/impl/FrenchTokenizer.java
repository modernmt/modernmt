package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.abbr.AbbreviationAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.FrenchTokenAnnotator;

import java.io.Reader;

public class FrenchTokenizer extends BaseTokenizer {

    public FrenchTokenizer() {
        super.annotators.add(AbbreviationAnnotator.getInstance(Language.FRENCH, true));
        super.annotators.add(new FrenchTokenAnnotator((Reader) null));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
