package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.lucene.LuceneTokenAnnotator;

import java.io.Reader;

public class TurkishTokenizer extends BaseTokenizer {

    public TurkishTokenizer() {
        super.annotators.add(LuceneTokenAnnotator.forLanguage(Language.TURKISH));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
