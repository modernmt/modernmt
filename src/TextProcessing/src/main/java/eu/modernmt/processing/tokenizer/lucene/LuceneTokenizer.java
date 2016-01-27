package eu.modernmt.processing.tokenizer.lucene;


import eu.modernmt.processing.tokenizer.lucene.analyzers.*;
import eu.modernmt.processing.tokenizer.util.LuceneTokenizerAdapter;
import eu.modernmt.processing.Languages;
import org.apache.lucene.analysis.Analyzer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 13/11/15.
 */
public class LuceneTokenizer extends LuceneTokenizerAdapter {

    public static final LuceneTokenizer ARABIC = new LuceneTokenizer(ArabicAnalyzer.class);
    public static final LuceneTokenizer GERMAN = new LuceneTokenizer(GermanAnalyzer.class);
    public static final LuceneTokenizer PERSIAN = new LuceneTokenizer(PersianAnalyzer.class);
    public static final LuceneTokenizer HINDI = new LuceneTokenizer(HindiAnalyzer.class);
    public static final LuceneTokenizer THAI = new LuceneTokenizer(ThaiAnalyzer.class);

    public static final LuceneTokenizer BULGARIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer BRAZILIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer CATALAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer CZECH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer DANISH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer GREEK = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer ENGLISH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer SPANISH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer BASQUE = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer FINNISH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer FRENCH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer IRISH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer GALICIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer HUNGARIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer ARMENIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer INDONESIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer ITALIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer LATVIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer DUTCH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer NORWEGIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer PORTUGUESE = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer ROMANIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer RUSSIAN = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer SWEDISH = new LuceneTokenizer(LiteStandardAnalyzer.class);
    public static final LuceneTokenizer TURKISH = new LuceneTokenizer(LiteStandardAnalyzer.class);

    public static final Map<Locale, LuceneTokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.ARABIC, ARABIC);
        ALL.put(Languages.GERMAN, GERMAN);
        ALL.put(Languages.PERSIAN, PERSIAN);
        ALL.put(Languages.HINDI, HINDI);
        ALL.put(Languages.THAI, THAI);
        ALL.put(Languages.BULGARIAN, BULGARIAN);
        ALL.put(Languages.BRAZILIAN, BRAZILIAN);
        ALL.put(Languages.CATALAN, CATALAN);
        ALL.put(Languages.CZECH, CZECH);
        ALL.put(Languages.DANISH, DANISH);
        ALL.put(Languages.GREEK, GREEK);
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.SPANISH, SPANISH);
        ALL.put(Languages.BASQUE, BASQUE);
        ALL.put(Languages.FINNISH, FINNISH);
        ALL.put(Languages.FRENCH, FRENCH);
        ALL.put(Languages.IRISH, IRISH);
        ALL.put(Languages.GALICIAN, GALICIAN);
        ALL.put(Languages.HUNGARIAN, HUNGARIAN);
        ALL.put(Languages.ARMENIAN, ARMENIAN);
        ALL.put(Languages.INDONESIAN, INDONESIAN);
        ALL.put(Languages.ITALIAN, ITALIAN);
        ALL.put(Languages.LATVIAN, LATVIAN);
        ALL.put(Languages.DUTCH, DUTCH);
        ALL.put(Languages.NORWEGIAN, NORWEGIAN);
        ALL.put(Languages.PORTUGUESE, PORTUGUESE);
        ALL.put(Languages.ROMANIAN, ROMANIAN);
        ALL.put(Languages.RUSSIAN, RUSSIAN);
        ALL.put(Languages.SWEDISH, SWEDISH);
        ALL.put(Languages.TURKISH, TURKISH);
    }

    private LuceneTokenizer(Class<? extends Analyzer> analyzerClass) {
        super(analyzerClass);
    }

}
