package eu.modernmt.context.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.DelegatingAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.LimitTokenCountAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CorpusAnalyzer extends DelegatingAnalyzerWrapper {

    private static final int MAX_INDEXED_WORDS_PER_DOCUMENT = 100000000;
    private static final Map<String, Class<? extends Analyzer>> ANALYZERS;

    static {
        HashMap<String, Class<? extends Analyzer>> analyzers = new HashMap<>();

        analyzers.put("en", org.apache.lucene.analysis.en.EnglishAnalyzer.class);
        analyzers.put("it", org.apache.lucene.analysis.it.ItalianAnalyzer.class);
        analyzers.put("ga", org.apache.lucene.analysis.en.EnglishAnalyzer.class);
        analyzers.put("ar", org.apache.lucene.analysis.ar.ArabicAnalyzer.class);
        analyzers.put("bg", org.apache.lucene.analysis.bg.BulgarianAnalyzer.class);
        analyzers.put("pt-BR", org.apache.lucene.analysis.br.BrazilianAnalyzer.class);
        analyzers.put("pt", org.apache.lucene.analysis.br.BrazilianAnalyzer.class);
        analyzers.put("ca", org.apache.lucene.analysis.ca.CatalanAnalyzer.class);
        analyzers.put("zh", org.apache.lucene.analysis.cjk.CJKAnalyzer.class);
        analyzers.put("ja", org.apache.lucene.analysis.cjk.CJKAnalyzer.class);
        analyzers.put("ko", org.apache.lucene.analysis.cjk.CJKAnalyzer.class);
        analyzers.put("da", org.apache.lucene.analysis.da.DanishAnalyzer.class);
        analyzers.put("de", org.apache.lucene.analysis.de.GermanAnalyzer.class);
        analyzers.put("de-CH", org.apache.lucene.analysis.de.GermanAnalyzer.class);
        analyzers.put("el", org.apache.lucene.analysis.el.GreekAnalyzer.class);
        analyzers.put("es", org.apache.lucene.analysis.es.SpanishAnalyzer.class);
        analyzers.put("fa", org.apache.lucene.analysis.fa.PersianAnalyzer.class);
        analyzers.put("fi", org.apache.lucene.analysis.fi.FinnishAnalyzer.class);
        analyzers.put("fr", org.apache.lucene.analysis.fr.FrenchAnalyzer.class);
        analyzers.put("fr-CA", org.apache.lucene.analysis.fr.FrenchAnalyzer.class);
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
        analyzers.put("nl-BE", org.apache.lucene.analysis.nl.DutchAnalyzer.class);
        analyzers.put("th", org.apache.lucene.analysis.th.ThaiAnalyzer.class);
        analyzers.put("cs", org.apache.lucene.analysis.cz.CzechAnalyzer.class);


        ANALYZERS = Collections.unmodifiableMap(analyzers);
    }

    private static Analyzer getByLanguage(Locale lang) {
        String tag = lang.toLanguageTag();

        Class<? extends Analyzer> analyzerClass = ANALYZERS.get(tag);
        if (analyzerClass == null)
            analyzerClass = ANALYZERS.get(tag.substring(0, 2));

        if (analyzerClass == null)
            analyzerClass = StandardAnalyzer.class;

        Analyzer analyzer;

        try {
            analyzer = analyzerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            throw new RuntimeException("Unable to instantiate class " + analyzerClass.getCanonicalName(), e);
        }

        return new LimitTokenCountAnalyzer(analyzer, MAX_INDEXED_WORDS_PER_DOCUMENT, false);
    }

    public CorpusAnalyzer() {
        super(PER_FIELD_REUSE_STRATEGY);
    }

    @Override
    protected Analyzer getWrappedAnalyzer(String fieldName) {
        Locale language = DocumentBuilder.getContentFieldLanguage(fieldName);
        return language == null ? new StandardAnalyzer() : getByLanguage(language);
    }

}
