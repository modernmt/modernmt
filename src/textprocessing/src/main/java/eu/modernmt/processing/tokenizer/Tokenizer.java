package eu.modernmt.processing.tokenizer;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.tokenizer.impl.*;

import java.util.HashMap;
import java.util.Map;

public class Tokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private static final HashMap<String, Class<? extends BaseTokenizer>> TOKENIZERS = new HashMap<>();

    static {
        TOKENIZERS.put(Language.ARABIC.getLanguage(), ArabicTokenizer.class);
        TOKENIZERS.put(Language.ARMENIAN.getLanguage(), ArmenianTokenizer.class);
        TOKENIZERS.put(Language.BASQUE.getLanguage(), BasqueTokenizer.class);
        TOKENIZERS.put(Language.BRETON.getLanguage(), BretonTokenizer.class);
        TOKENIZERS.put(Language.BULGARIAN.getLanguage(), BulgarianTokenizer.class);
        TOKENIZERS.put(Language.CATALAN.getLanguage(), CatalanTokenizer.class);
        TOKENIZERS.put(Language.KHMER.getLanguage(), CentralKhmerTokenizer.class);
        TOKENIZERS.put(Language.CHINESE.getLanguage(), ChineseTokenizer.class);
        TOKENIZERS.put(Language.CZECH.getLanguage(), CzechTokenizer.class);
        TOKENIZERS.put(Language.DANISH.getLanguage(), DanishTokenizer.class);
        TOKENIZERS.put(Language.DUTCH.getLanguage(), DutchTokenizer.class);
        TOKENIZERS.put(Language.ENGLISH.getLanguage(), EnglishTokenizer.class);
        TOKENIZERS.put(Language.ESPERANTO.getLanguage(), EsperantoTokenizer.class);
        TOKENIZERS.put(Language.PERSIAN.getLanguage(), FarsiTokenizer.class);
        TOKENIZERS.put(Language.FINNISH.getLanguage(), FinnishTokenizer.class);
        TOKENIZERS.put(Language.FRENCH.getLanguage(), FrenchTokenizer.class);
        TOKENIZERS.put(Language.GALICIAN.getLanguage(), GalicianTokenizer.class);
        TOKENIZERS.put(Language.GERMAN.getLanguage(), GermanTokenizer.class);
        TOKENIZERS.put(Language.GREEK.getLanguage(), GreekTokenizer.class);
        TOKENIZERS.put(Language.HEBREW.getLanguage(), HebrewTokenizer.class);
        TOKENIZERS.put(Language.HINDI.getLanguage(), HindiTokenizer.class);
        TOKENIZERS.put(Language.HUNGARIAN.getLanguage(), HungarianTokenizer.class);
        TOKENIZERS.put(Language.ICELANDIC.getLanguage(), IcelandicTokenizer.class);
        TOKENIZERS.put(Language.INDONESIAN.getLanguage(), IndonesianTokenizer.class);
        TOKENIZERS.put(Language.IRISH.getLanguage(), IrishTokenizer.class);
        TOKENIZERS.put(Language.ITALIAN.getLanguage(), ItalianTokenizer.class);
        TOKENIZERS.put(Language.JAPANESE.getLanguage(), JapaneseTokenizer.class);
        TOKENIZERS.put(Language.KOREAN.getLanguage(), KoreanTokenizer.class);
        TOKENIZERS.put(Language.LATVIAN.getLanguage(), LatvianTokenizer.class);
        TOKENIZERS.put(Language.MALAYALAM.getLanguage(), MalayalamTokenizer.class);
        TOKENIZERS.put(Language.NORWEGIAN.getLanguage(), NorwegianTokenizer.class);
        TOKENIZERS.put(Language.NORTHERN_SAMI.getLanguage(), NorthernSamiTokenizer.class);
        TOKENIZERS.put(Language.POLISH.getLanguage(), PolishTokenizer.class);
        TOKENIZERS.put(Language.PORTUGUESE.getLanguage(), PortugueseTokenizer.class);
        TOKENIZERS.put(Language.ROMANIAN.getLanguage(), RomanianTokenizer.class);
        TOKENIZERS.put(Language.RUSSIAN.getLanguage(), RussianTokenizer.class);
        TOKENIZERS.put(Language.SLOVAK.getLanguage(), SlovakTokenizer.class);
        TOKENIZERS.put(Language.SLOVENE.getLanguage(), SloveneTokenizer.class);
        TOKENIZERS.put(Language.SPANISH.getLanguage(), SpanishTokenizer.class);
        TOKENIZERS.put(Language.SWEDISH.getLanguage(), SwedishTokenizer.class);
        TOKENIZERS.put(Language.TAGALOG.getLanguage(), TagalogTokenizer.class);
        TOKENIZERS.put(Language.TAMIL.getLanguage(), TamilTokenizer.class);
        TOKENIZERS.put(Language.THAI.getLanguage(), ThaiTokenizer.class);
        TOKENIZERS.put(Language.TURKISH.getLanguage(), TurkishTokenizer.class);
        TOKENIZERS.put(Language.UKRAINIAN.getLanguage(), RussianTokenizer.class);
    }

    private final BaseTokenizer tokenizer;

    public Tokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        Class<? extends BaseTokenizer> clazz = TOKENIZERS.get(sourceLanguage.getLanguage());
        if (clazz == null) {
            this.tokenizer = new DefaultTokenizer();
        } else {
            this.tokenizer = TextProcessor.newInstance(clazz, sourceLanguage, targetLanguage);
        }

    }

    @Override
    public SentenceBuilder call(SentenceBuilder sentence, Map<String, Object> metadata) {
        return tokenizer.call(sentence, metadata);
    }

}
