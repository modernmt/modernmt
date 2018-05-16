package eu.modernmt.processing.tokenizer;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.tokenizer.impl.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Tokenizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private static final HashMap<Language, Class<? extends BaseTokenizer>> TOKENIZERS = new HashMap<>();

    static {
        TOKENIZERS.put(Language.ARABIC, ArabicTokenizer.class);
        TOKENIZERS.put(Language.ARMENIAN, ArmenianTokenizer.class);
        TOKENIZERS.put(Language.BASQUE, BasqueTokenizer.class);
        TOKENIZERS.put(Language.BRETON, BretonTokenizer.class);
        TOKENIZERS.put(Language.BULGARIAN, BulgarianTokenizer.class);
        TOKENIZERS.put(Language.CATALAN, CatalanTokenizer.class);
        TOKENIZERS.put(Language.KHMER, CentralKhmerTokenizer.class);
        TOKENIZERS.put(Language.CHINESE, ChineseTokenizer.class);
        TOKENIZERS.put(Language.CZECH, CzechTokenizer.class);
        TOKENIZERS.put(Language.DANISH, DanishTokenizer.class);
        TOKENIZERS.put(Language.DUTCH, DutchTokenizer.class);
        TOKENIZERS.put(Language.ENGLISH, EnglishTokenizer.class);
        TOKENIZERS.put(Language.ESPERANTO, EsperantoTokenizer.class);
        TOKENIZERS.put(Language.PERSIAN, FarsiTokenizer.class);
        TOKENIZERS.put(Language.FINNISH, FinnishTokenizer.class);
        TOKENIZERS.put(Language.FRENCH, FrenchTokenizer.class);
        TOKENIZERS.put(Language.GALICIAN, GalicianTokenizer.class);
        TOKENIZERS.put(Language.GERMAN, GermanTokenizer.class);
        TOKENIZERS.put(Language.GREEK, GreekTokenizer.class);
        TOKENIZERS.put(Language.HEBREW, HebrewTokenizer.class);
        TOKENIZERS.put(Language.HINDI, HindiTokenizer.class);
        TOKENIZERS.put(Language.HUNGARIAN, HungarianTokenizer.class);
        TOKENIZERS.put(Language.ICELANDIC, IcelandicTokenizer.class);
        TOKENIZERS.put(Language.INDONESIAN, IndonesianTokenizer.class);
        TOKENIZERS.put(Language.IRISH, IrishTokenizer.class);
        TOKENIZERS.put(Language.ITALIAN, ItalianTokenizer.class);
        TOKENIZERS.put(Language.JAPANESE, JapaneseTokenizer.class);
        TOKENIZERS.put(Language.KOREAN, KoreanTokenizer.class);
        TOKENIZERS.put(Language.LATVIAN, LatvianTokenizer.class);
        TOKENIZERS.put(Language.MALAYALAM, MalayalamTokenizer.class);
        TOKENIZERS.put(Language.NORWEGIAN, NorwegianTokenizer.class);
        TOKENIZERS.put(Language.NORTHERN_SAMI, NorthernSamiTokenizer.class);
        TOKENIZERS.put(Language.POLISH, PolishTokenizer.class);
        TOKENIZERS.put(Language.PORTUGUESE, PortugueseTokenizer.class);
        TOKENIZERS.put(Language.ROMANIAN, RomanianTokenizer.class);
        TOKENIZERS.put(Language.RUSSIAN, RussianTokenizer.class);
        TOKENIZERS.put(Language.SLOVAK, SlovakTokenizer.class);
        TOKENIZERS.put(Language.SLOVENE, SloveneTokenizer.class);
        TOKENIZERS.put(Language.SPANISH, SpanishTokenizer.class);
        TOKENIZERS.put(Language.SWEDISH, SwedishTokenizer.class);
        TOKENIZERS.put(Language.TAGALOG, TagalogTokenizer.class);
        TOKENIZERS.put(Language.TAMIL, TamilTokenizer.class);
        TOKENIZERS.put(Language.THAI, ThaiTokenizer.class);
        TOKENIZERS.put(Language.TURKISH, TurkishTokenizer.class);
        TOKENIZERS.put(Language.UKRAINIAN, UkrainianTokenizer.class);
    }

    private final BaseTokenizer tokenizer;

    public Tokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        Class<? extends BaseTokenizer> clazz = TOKENIZERS.get(sourceLanguage);
        if (clazz == null) {
            this.tokenizer = new DefaultTokenizer(sourceLanguage, targetLanguage);
        } else {
            Constructor<? extends BaseTokenizer> constructor;

            try {
                constructor = clazz.getConstructor(Language.class, Language.class);
            } catch (NoSuchMethodException e) {
                throw new Error(e);
            }

            try {
                this.tokenizer = constructor.newInstance(sourceLanguage, targetLanguage);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new Error(e);
            } catch (InvocationTargetException e) {
                Throwable throwable = e.getCause();

                if (throwable instanceof UnsupportedLanguageException)
                    throw (UnsupportedLanguageException) throwable;
                else if (throwable instanceof RuntimeException)
                    throw (RuntimeException) throwable;
                else
                    throw new Error(throwable);
            }
        }
    }

    @Override
    public SentenceBuilder call(SentenceBuilder sentence, Map<String, Object> metadata) throws ProcessingException {
        return tokenizer.call(sentence, metadata);
    }
    
}
