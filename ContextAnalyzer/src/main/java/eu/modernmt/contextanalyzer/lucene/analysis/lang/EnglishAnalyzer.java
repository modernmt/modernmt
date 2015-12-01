package eu.modernmt.contextanalyzer.lucene.analysis.lang;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class EnglishAnalyzer extends LanguageAnalyzer {

    public EnglishAnalyzer(AnalyzerConfig config) {
        super(config, org.apache.lucene.analysis.en.EnglishAnalyzer.getDefaultStopSet());
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();

        TokenStream result = new StandardFilter(source);

        if (config.removeElisions)
            result = new EnglishPossessiveFilter(result);
        if (config.toLowerCase)
            result = new LowerCaseFilter(result);
        if (config.filterStopWords)
            result = new StopFilter(result, stopwords);
        if (config.enableStemming) {
            if (stemmingExclusionSet != null && !stemmingExclusionSet.isEmpty())
                result = new SetKeywordMarkerFilter(result, stemmingExclusionSet);

            result = new PorterStemFilter(result);
        }

        return new TokenStreamComponents(source, result);
    }

}
