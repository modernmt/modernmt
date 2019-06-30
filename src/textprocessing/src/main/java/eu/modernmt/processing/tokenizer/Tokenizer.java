package eu.modernmt.processing.tokenizer;

import eu.modernmt.lang.Language2;
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
        TOKENIZERS.put(Language2.ARABIC.getLanguage(), ArabicTokenizer.class);
        TOKENIZERS.put(Language2.ARMENIAN.getLanguage(), ArmenianTokenizer.class);
        TOKENIZERS.put(Language2.BASQUE.getLanguage(), BasqueTokenizer.class);
        TOKENIZERS.put(Language2.BRETON.getLanguage(), BretonTokenizer.class);
        TOKENIZERS.put(Language2.BULGARIAN.getLanguage(), BulgarianTokenizer.class);
        TOKENIZERS.put(Language2.CATALAN.getLanguage(), CatalanTokenizer.class);
        TOKENIZERS.put(Language2.KHMER.getLanguage(), CentralKhmerTokenizer.class);
        TOKENIZERS.put(Language2.CHINESE.getLanguage(), ChineseTokenizer.class);
        TOKENIZERS.put(Language2.CZECH.getLanguage(), CzechTokenizer.class);
        TOKENIZERS.put(Language2.DANISH.getLanguage(), DanishTokenizer.class);
        TOKENIZERS.put(Language2.DUTCH.getLanguage(), DutchTokenizer.class);
        TOKENIZERS.put(Language2.ENGLISH.getLanguage(), EnglishTokenizer.class);
        TOKENIZERS.put(Language2.ESPERANTO.getLanguage(), EsperantoTokenizer.class);
        TOKENIZERS.put(Language2.PERSIAN.getLanguage(), FarsiTokenizer.class);
        TOKENIZERS.put(Language2.FINNISH.getLanguage(), FinnishTokenizer.class);
        TOKENIZERS.put(Language2.FRENCH.getLanguage(), FrenchTokenizer.class);
        TOKENIZERS.put(Language2.GALICIAN.getLanguage(), GalicianTokenizer.class);
        TOKENIZERS.put(Language2.GERMAN.getLanguage(), GermanTokenizer.class);
        TOKENIZERS.put(Language2.GREEK.getLanguage(), GreekTokenizer.class);
        TOKENIZERS.put(Language2.HEBREW.getLanguage(), HebrewTokenizer.class);
        TOKENIZERS.put(Language2.HINDI.getLanguage(), HindiTokenizer.class);
        TOKENIZERS.put(Language2.HUNGARIAN.getLanguage(), HungarianTokenizer.class);
        TOKENIZERS.put(Language2.ICELANDIC.getLanguage(), IcelandicTokenizer.class);
        TOKENIZERS.put(Language2.INDONESIAN.getLanguage(), IndonesianTokenizer.class);
        TOKENIZERS.put(Language2.IRISH.getLanguage(), IrishTokenizer.class);
        TOKENIZERS.put(Language2.ITALIAN.getLanguage(), ItalianTokenizer.class);
        TOKENIZERS.put(Language2.JAPANESE.getLanguage(), JapaneseTokenizer.class);
        TOKENIZERS.put(Language2.KOREAN.getLanguage(), KoreanTokenizer.class);
        TOKENIZERS.put(Language2.LATVIAN.getLanguage(), LatvianTokenizer.class);
        TOKENIZERS.put(Language2.MALAYALAM.getLanguage(), MalayalamTokenizer.class);
        TOKENIZERS.put(Language2.NORWEGIAN.getLanguage(), NorwegianTokenizer.class);
        TOKENIZERS.put(Language2.NORTHERN_SAMI.getLanguage(), NorthernSamiTokenizer.class);
        TOKENIZERS.put(Language2.POLISH.getLanguage(), PolishTokenizer.class);
        TOKENIZERS.put(Language2.PORTUGUESE.getLanguage(), PortugueseTokenizer.class);
        TOKENIZERS.put(Language2.ROMANIAN.getLanguage(), RomanianTokenizer.class);
        TOKENIZERS.put(Language2.RUSSIAN.getLanguage(), RussianTokenizer.class);
        TOKENIZERS.put(Language2.SLOVAK.getLanguage(), SlovakTokenizer.class);
        TOKENIZERS.put(Language2.SLOVENE.getLanguage(), SloveneTokenizer.class);
        TOKENIZERS.put(Language2.SPANISH.getLanguage(), SpanishTokenizer.class);
        TOKENIZERS.put(Language2.SWEDISH.getLanguage(), SwedishTokenizer.class);
        TOKENIZERS.put(Language2.TAGALOG.getLanguage(), TagalogTokenizer.class);
        TOKENIZERS.put(Language2.TAMIL.getLanguage(), TamilTokenizer.class);
        TOKENIZERS.put(Language2.THAI.getLanguage(), ThaiTokenizer.class);
        TOKENIZERS.put(Language2.TURKISH.getLanguage(), TurkishTokenizer.class);
        TOKENIZERS.put(Language2.UKRAINIAN.getLanguage(), RussianTokenizer.class);
    }

    private final BaseTokenizer tokenizer;

    public Tokenizer(Language2 sourceLanguage, Language2 targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        Class<? extends BaseTokenizer> clazz = TOKENIZERS.get(sourceLanguage.getLanguage());
        if (clazz == null) {
            this.tokenizer = new DefaultTokenizer();
        } else {
            try {
                this.tokenizer = TextProcessor.newInstance(clazz, sourceLanguage, targetLanguage);
            } catch (ProcessingException e) {
                throw new Error(e);
            }
        }

    }

    @Override
    public SentenceBuilder call(SentenceBuilder sentence, Map<String, Object> metadata) throws ProcessingException {
        return tokenizer.call(sentence, metadata);
    }

}
