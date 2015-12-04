package eu.modernmt.context.lucene.analysis.lang;

import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class LanguageAnalyzer extends StopwordAnalyzerBase {

    private static final Map<String, Class<? extends LanguageAnalyzer>> ANALYZERS;

    static {
        HashMap<String, Class<? extends LanguageAnalyzer>> analyzers = new HashMap<>();

        analyzers.put("it", ItalianAnalyzer.class);
        analyzers.put("en", EnglishAnalyzer.class);

        ANALYZERS = Collections.unmodifiableMap(analyzers);
    }

    public static final class AnalyzerConfig {

        public boolean enableStemming = true;
        public CharArraySet stemmingExclusionSet = null;

        public boolean filterStopWords = true;
        public CharArraySet stopWordsSet = null;

        public boolean toLowerCase = true;

        public boolean removeElisions = true;

    }

    protected final AnalyzerConfig config;
    protected final CharArraySet stemmingExclusionSet;

    protected LanguageAnalyzer(AnalyzerConfig config, CharArraySet defaultStopWordsSet) {
        super(config.stopWordsSet == null ? defaultStopWordsSet : config.stopWordsSet);

        if (config.enableStemming && config.stemmingExclusionSet != null)
            this.stemmingExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(config.stemmingExclusionSet));
        else
            this.stemmingExclusionSet = null;

        this.config = config;
    }

    public static LanguageAnalyzer getByISOLanguage(String lang) {
        return getByISOLanguage(lang, new AnalyzerConfig());
    }

    public static LanguageAnalyzer getByISOLanguage(String lang, AnalyzerConfig config) {
        Class<? extends LanguageAnalyzer> analyzerClass = ANALYZERS.get(lang);
        if (analyzerClass == null)
            analyzerClass = DefaultAnalyzer.class;

        try {
            return analyzerClass.getConstructor(AnalyzerConfig.class).newInstance(config);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            throw new RuntimeException("Unable to instantiate class " + analyzerClass.getCanonicalName(), e);
        }
    }

}
