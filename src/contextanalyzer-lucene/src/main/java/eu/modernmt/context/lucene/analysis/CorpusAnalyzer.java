package eu.modernmt.context.lucene.analysis;

import eu.modernmt.context.lucene.analysis.lang.LanguageAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.Locale;

public class CorpusAnalyzer extends DelegatingAnalyzerWrapper {

    private static final int MAX_INDEXED_WORDS_PER_DOCUMENT = 100000000;

    public CorpusAnalyzer() {
        super(PER_FIELD_REUSE_STRATEGY);
    }

    @Override
    protected Analyzer getWrappedAnalyzer(String fieldName) {
        Analyzer analyzer;

        Locale language = DocumentBuilder.getContentFieldLanguage(fieldName);
        if (language == null)
            analyzer = new StandardAnalyzer();
        else
            analyzer = new LimitTokenCountAnalyzer(LanguageAnalyzer.getByLanguage(language), MAX_INDEXED_WORDS_PER_DOCUMENT, false);

        return analyzer;
    }

}
