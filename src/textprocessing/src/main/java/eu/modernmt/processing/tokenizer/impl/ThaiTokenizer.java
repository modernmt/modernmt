package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.lucene.LuceneTokenAnnotator;

import java.io.Reader;

public class ThaiTokenizer extends BaseTokenizer {

    public ThaiTokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        super.annotators.add(LuceneTokenAnnotator.forLanguage(sourceLanguage));
        super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
    }
}
