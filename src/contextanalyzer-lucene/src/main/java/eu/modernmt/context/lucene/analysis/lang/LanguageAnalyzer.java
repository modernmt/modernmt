package eu.modernmt.context.lucene.analysis.lang;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class LanguageAnalyzer extends StopwordAnalyzerBase {

    private static final Map<String, Class<? extends Analyzer>> ANALYZERS;

    static {
        HashMap<String, Class<? extends Analyzer>> analyzers = new HashMap<>();

        analyzers.put("it", ItalianAnalyzer.class);
        analyzers.put("en", EnglishAnalyzer.class);
        analyzers.put("ga", EnglishAnalyzer.class);

        // Adapters

        analyzers.put("ar", org.apache.lucene.analysis.ar.ArabicAnalyzer.class);
        analyzers.put("bg", org.apache.lucene.analysis.bg.BulgarianAnalyzer.class);
        analyzers.put("pt", org.apache.lucene.analysis.br.BrazilianAnalyzer.class);
        analyzers.put("ca", org.apache.lucene.analysis.ca.CatalanAnalyzer.class);
        analyzers.put("zh", org.apache.lucene.analysis.cjk.CJKAnalyzer.class);
        analyzers.put("ja", org.apache.lucene.analysis.cjk.CJKAnalyzer.class);
        analyzers.put("ko", org.apache.lucene.analysis.cjk.CJKAnalyzer.class);
        analyzers.put("da", org.apache.lucene.analysis.da.DanishAnalyzer.class);
        analyzers.put("de", org.apache.lucene.analysis.de.GermanAnalyzer.class);
        analyzers.put("el", org.apache.lucene.analysis.el.GreekAnalyzer.class);
        analyzers.put("es", org.apache.lucene.analysis.es.SpanishAnalyzer.class);
        analyzers.put("fa", org.apache.lucene.analysis.fa.PersianAnalyzer.class);
        analyzers.put("fi", org.apache.lucene.analysis.fi.FinnishAnalyzer.class);
        analyzers.put("fr", org.apache.lucene.analysis.fr.FrenchAnalyzer.class);
        analyzers.put("hi", org.apache.lucene.analysis.hi.HindiAnalyzer.class);
        analyzers.put("hu", org.apache.lucene.analysis.hu.HungarianAnalyzer.class);
        analyzers.put("hy", org.apache.lucene.analysis.hy.ArmenianAnalyzer.class);
        analyzers.put("id", org.apache.lucene.analysis.id.IndonesianAnalyzer.class);
        analyzers.put("lv", org.apache.lucene.analysis.lv.LatvianAnalyzer.class);
        analyzers.put("no", org.apache.lucene.analysis.no.NorwegianAnalyzer.class);
        analyzers.put("ro", org.apache.lucene.analysis.ro.RomanianAnalyzer.class);
        analyzers.put("tr", org.apache.lucene.analysis.tr.TurkishAnalyzer.class);
        analyzers.put("ru", org.apache.lucene.analysis.ru.RussianAnalyzer.class);
        analyzers.put("sv", org.apache.lucene.analysis.sv.SwedishAnalyzer.class);
        analyzers.put("nl", org.apache.lucene.analysis.nl.DutchAnalyzer.class);
        analyzers.put("th", org.apache.lucene.analysis.th.ThaiAnalyzer.class);
        analyzers.put("cs", org.apache.lucene.analysis.cz.CzechAnalyzer.class);


        ANALYZERS = Collections.unmodifiableMap(analyzers);
    }

    public static final class AnalyzerConfig {

        public boolean enableStemming = false;
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

    public static Analyzer getByLanguage(String language) {
        return getByLanguage(language, new AnalyzerConfig());
    }

    public static Analyzer getByLanguage(String language, Analyzer def) {
        return getByLanguage(language, new AnalyzerConfig(), def);
    }

    public static Analyzer getByLanguage(String language, AnalyzerConfig config) {
        return getByLanguage(language, config, new DefaultAnalyzer(config));
    }

    public static Analyzer getByLanguage(String language, AnalyzerConfig config, Analyzer def) {
        Class<? extends Analyzer> analyzerClass = ANALYZERS.get(language);
        if (analyzerClass == null)
            return def;

        Analyzer analyzer;

        try {
            if (LanguageAnalyzer.class.isAssignableFrom(analyzerClass))
                analyzer = analyzerClass.getConstructor(AnalyzerConfig.class).newInstance(config);
            else
                analyzer = analyzerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            throw new RuntimeException("Unable to instantiate class " + analyzerClass.getCanonicalName(), e);
        }

        return analyzer;
    }

}
