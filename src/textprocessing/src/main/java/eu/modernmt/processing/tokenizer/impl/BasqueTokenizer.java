package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.lucene.LuceneTokenAnnotator;

import java.io.Reader;

public class BasqueTokenizer extends BaseTokenizer {

    public BasqueTokenizer() {
        super.annotators.add(LuceneTokenAnnotator.forLanguage(Language.BASQUE));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
