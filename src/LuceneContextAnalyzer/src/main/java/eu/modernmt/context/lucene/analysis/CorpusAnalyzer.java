package eu.modernmt.context.lucene.analysis;

import eu.modernmt.context.lucene.DocumentBuilder;
import eu.modernmt.context.lucene.analysis.lang.LanguageAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.Locale;

public class CorpusAnalyzer extends DelegatingAnalyzerWrapper {

    private static final LanguageAnalyzer.AnalyzerConfig ANALYZER_CONFIG;

    static {
        ANALYZER_CONFIG = new LanguageAnalyzer.AnalyzerConfig();
        ANALYZER_CONFIG.toLowerCase = true;
        ANALYZER_CONFIG.enableStemming = false;
        ANALYZER_CONFIG.removeElisions = true;
    }

    public CorpusAnalyzer() {
        super(PER_FIELD_REUSE_STRATEGY);
    }

    @Override
    protected Analyzer getWrappedAnalyzer(String fieldName) {
        Analyzer baseAnalyzer;
        try {
            Locale lang = DocumentBuilder.getLangOfContentField(fieldName);
            baseAnalyzer = LanguageAnalyzer.getByLanguage(lang, ANALYZER_CONFIG);
        } catch (Throwable e) {
            baseAnalyzer = new StandardAnalyzer();
        }

        return baseAnalyzer;
    }

}
