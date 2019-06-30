package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language2;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.opennlp.OpenNLPTokenAnnotator;

import java.io.Reader;

public class NorthernSamiTokenizer extends BaseTokenizer {

    public NorthernSamiTokenizer() {
        super.annotators.add(OpenNLPTokenAnnotator.forLanguage(Language2.NORTHERN_SAMI));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
