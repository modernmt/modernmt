package eu.modernmt.tokenizer;

import eu.modernmt.tokenizer.corenlp.CoreNLPTokenizer;
import eu.modernmt.tokenizer.hebmorph.HebMorphTokenizer;
import eu.modernmt.tokenizer.kuromoji.KuromojiTokenizer;
import eu.modernmt.tokenizer.languagetool.LanguageToolTokenizer;
import eu.modernmt.tokenizer.lucene.LuceneTokenizer;
import eu.modernmt.tokenizer.moses.MosesTokenizer;
import eu.modernmt.tokenizer.opennlp.OpenNLPTokenizer;
import eu.modernmt.tokenizer.paoding.PaodingTokenizer;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by davide on 13/11/15.
 */
public class Tokenizers {

    private static final HashMap<Locale, ITokenizerFactory> tokenizers;

    private static void setTokenizer(Locale language, ITokenizerFactory factory) {
        ITokenizerFactory old = tokenizers.putIfAbsent(language, factory);

        if (old != null)
            throw new RuntimeException("Duplicate value for language " + language.getLanguage());
    }

    static {
        tokenizers = new HashMap<>();

        // Moses
        setTokenizer(Languages.CATALAN, MosesTokenizer.CATALAN);
        setTokenizer(Languages.CZECH, MosesTokenizer.CZECH);
        setTokenizer(Languages.GERMAN, MosesTokenizer.GERMAN);
        setTokenizer(Languages.GREEK, MosesTokenizer.GREEK);
        setTokenizer(Languages.ENGLISH, MosesTokenizer.ENGLISH);
        setTokenizer(Languages.SPANISH, MosesTokenizer.SPANISH);
        setTokenizer(Languages.FINNISH, MosesTokenizer.FINNISH);
        setTokenizer(Languages.FRENCH, MosesTokenizer.FRENCH);
        setTokenizer(Languages.HUNGARIAN, MosesTokenizer.HUNGARIAN);
        setTokenizer(Languages.ICELANDIC, MosesTokenizer.ICELANDIC);
        setTokenizer(Languages.ITALIAN, MosesTokenizer.ITALIAN);
        setTokenizer(Languages.LATVIAN, MosesTokenizer.LATVIAN);
        setTokenizer(Languages.DUTCH, MosesTokenizer.DUTCH);
        setTokenizer(Languages.POLISH, MosesTokenizer.POLISH);
        setTokenizer(Languages.PORTUGUESE, MosesTokenizer.PORTUGUESE);
        setTokenizer(Languages.ROMANIAN, MosesTokenizer.ROMANIAN);
        setTokenizer(Languages.RUSSIAN, MosesTokenizer.RUSSIAN);
        setTokenizer(Languages.SLOVAK, MosesTokenizer.SLOVAK);
        setTokenizer(Languages.SLOVENE, MosesTokenizer.SLOVENE);
        setTokenizer(Languages.SWEDISH, MosesTokenizer.SWEDISH);
        setTokenizer(Languages.TAMIL, MosesTokenizer.TAMIL);

        // CoreNLP
        setTokenizer(Languages.ARABIC, CoreNLPTokenizer.ARABIC);

        // OpenNLP
        setTokenizer(Languages.DANISH, OpenNLPTokenizer.DANISH);
        setTokenizer(Languages.NORTHERN_SAMI, OpenNLPTokenizer.NORTHERN_SAMI);

        // Lucene
        setTokenizer(Languages.PERSIAN, LuceneTokenizer.PERSIAN);
        setTokenizer(Languages.HINDI, LuceneTokenizer.HINDI);
        setTokenizer(Languages.THAI, LuceneTokenizer.THAI);
        setTokenizer(Languages.BULGARIAN, LuceneTokenizer.BULGARIAN);
        setTokenizer(Languages.BRAZILIAN, LuceneTokenizer.BRAZILIAN);
        setTokenizer(Languages.BASQUE, LuceneTokenizer.BASQUE);
        setTokenizer(Languages.IRISH, LuceneTokenizer.IRISH);
        setTokenizer(Languages.ARMENIAN, LuceneTokenizer.ARMENIAN);
        setTokenizer(Languages.INDONESIAN, LuceneTokenizer.INDONESIAN);
        setTokenizer(Languages.NORWEGIAN, LuceneTokenizer.NORWEGIAN);
        setTokenizer(Languages.TURKISH, LuceneTokenizer.TURKISH);

        // Language Tool
        setTokenizer(Languages.BRETON, LanguageToolTokenizer.BRETON);
        setTokenizer(Languages.ESPERANTO, LanguageToolTokenizer.ESPERANTO);
        setTokenizer(Languages.KHMER, LanguageToolTokenizer.KHMER);
        setTokenizer(Languages.MALAYALAM, LanguageToolTokenizer.MALAYALAM);
        setTokenizer(Languages.UKRAINIAN, LanguageToolTokenizer.UKRAINIAN);
        setTokenizer(Languages.GALICIAN, LanguageToolTokenizer.GALICIAN);
        setTokenizer(Languages.TAGALOG, LanguageToolTokenizer.TAGALOG);

        // Customs
        setTokenizer(Languages.HEBREW, HebMorphTokenizer.HEBREW);
        setTokenizer(Languages.JAPANESE, KuromojiTokenizer.JAPANESE);
        setTokenizer(Languages.CHINESE, PaodingTokenizer.CHINESE);
    }

    public static ITokenizerFactory getFactory(Locale locale) {
        ITokenizerFactory factory = tokenizers.get(locale);

        if (factory == null)
            throw new IllegalArgumentException("Unsupported language: " + locale.getLanguage());

        return factory;
    }

}
