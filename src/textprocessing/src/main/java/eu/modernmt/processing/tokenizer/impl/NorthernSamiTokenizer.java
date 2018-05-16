package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.opennlp.OpenNLPTokenAnnotator;

import java.io.Reader;

public class NorthernSamiTokenizer extends BaseTokenizer {

    public NorthernSamiTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        super.annotators.add(OpenNLPTokenAnnotator.forLanguage(sourceLanguage));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
