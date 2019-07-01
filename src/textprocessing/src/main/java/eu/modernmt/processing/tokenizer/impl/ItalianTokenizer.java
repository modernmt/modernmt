package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.abbr.AbbreviationAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.ItalianTokenAnnotator;

import java.io.Reader;

public class ItalianTokenizer extends BaseTokenizer {

    public ItalianTokenizer() {
        super.annotators.add(AbbreviationAnnotator.getInstance(Language.ITALIAN, true));
        super.annotators.add(new ItalianTokenAnnotator((Reader) null));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
