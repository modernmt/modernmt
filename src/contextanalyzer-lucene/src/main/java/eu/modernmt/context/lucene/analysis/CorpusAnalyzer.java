package eu.modernmt.context.lucene.analysis;

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

    private Locale language;

    public CorpusAnalyzer(Locale language) {
        super(PER_FIELD_REUSE_STRATEGY);
        this.language = language;
    }

    @Override
    protected Analyzer getWrappedAnalyzer(String fieldName) {
        Analyzer baseAnalyzer;
        try {
            baseAnalyzer = LanguageAnalyzer.getByLanguage(language, ANALYZER_CONFIG);
        } catch (Throwable e) {
            baseAnalyzer = new StandardAnalyzer();
        }

        return baseAnalyzer;
    }

}
