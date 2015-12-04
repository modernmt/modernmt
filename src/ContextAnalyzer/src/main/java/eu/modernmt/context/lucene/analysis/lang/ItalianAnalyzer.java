package eu.modernmt.context.lucene.analysis.lang;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.it.ItalianLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ElisionFilter;

import java.util.Arrays;

public class ItalianAnalyzer extends LanguageAnalyzer {

    private static final CharArraySet DEFAULT_ARTICLES = CharArraySet.unmodifiableSet(new CharArraySet(Arrays.asList("c", "l", "all", "dall", "dell", "nell",
            "sull", "coll", "pell", "gl", "agl", "dagl", "degl", "negl", "sugl", "un", "m", "t", "s", "v", "d"), true));

    public ItalianAnalyzer(AnalyzerConfig config) {
        super(config, org.apache.lucene.analysis.it.ItalianAnalyzer.getDefaultStopSet());
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final Tokenizer source = new StandardTokenizer();

        TokenStream result = new StandardFilter(source);

        if (config.removeElisions)
            result = new ElisionFilter(result, DEFAULT_ARTICLES);
        if (config.toLowerCase)
            result = new LowerCaseFilter(result);
        if (config.filterStopWords)
            result = new StopFilter(result, stopwords);
        if (config.enableStemming) {
            if (stemmingExclusionSet != null && !stemmingExclusionSet.isEmpty())
                result = new SetKeywordMarkerFilter(result, stemmingExclusionSet);

            result = new ItalianLightStemFilter(result);
        }

        return new TokenStreamComponents(source, result);
    }

}
