package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.Languages;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 27/01/16.
 */
public class Tokenizers {

    private static class TokenizerLoader {

        private Map<Locale, Tokenizer> tokenizers = null;
        private String className;

        public TokenizerLoader(String className) {
            this.className = className;
        }

        @SuppressWarnings("unchecked")
        private Map<Locale, Tokenizer> getTokenizers() {
            if (tokenizers == null) {
                synchronized (this) {
                    if (tokenizers == null) {
                        try {
                            Class<?> c = Class.forName(this.className);
                            Field field = c.getDeclaredField("ALL");
                            tokenizers = (Map<Locale, Tokenizer>) field.get(null);
                        } catch (Exception e) {
                            throw new Error("Unable to load class " + this.className, e);
                        }
                    }
                }
            }

            return tokenizers;
        }

        public Tokenizer load(Locale locale) {
            return getTokenizers().get(locale);
        }

    }

    private static final TokenizerLoader CoreNLP = new TokenizerLoader("eu.modernmt.processing.tokenizer.corenlp.CoreNLPTokenizer");
    private static final TokenizerLoader HebMorph = new TokenizerLoader("eu.modernmt.processing.tokenizer.hebmorph.HebMorphTokenizer");
    private static final TokenizerLoader Kuromoji = new TokenizerLoader("eu.modernmt.processing.tokenizer.kuromoji.KuromojiTokenizer");
    private static final TokenizerLoader LanguageTool = new TokenizerLoader("eu.modernmt.processing.tokenizer.languagetool.LanguageToolTokenizer");
    private static final TokenizerLoader Lucene = new TokenizerLoader("eu.modernmt.processing.tokenizer.lucene.LuceneTokenizer");
    private static final TokenizerLoader Moses = new TokenizerLoader("eu.modernmt.processing.tokenizer.moses.MosesTokenizer");
    private static final TokenizerLoader OpenNLP = new TokenizerLoader("eu.modernmt.processing.tokenizer.opennlp.OpenNLPTokenizer");
    private static final TokenizerLoader Paoding = new TokenizerLoader("eu.modernmt.processing.tokenizer.paoding.PaodingTokenizer");

    private static final HashMap<Locale, TokenizerLoader> tokenizers;

    private static void setTokenizer(Locale language, TokenizerLoader loader) {
        TokenizerLoader old = tokenizers.putIfAbsent(language, loader);

        if (old != null)
            throw new RuntimeException("Duplicate value for language " + language.getLanguage());
    }

    static {
        tokenizers = new HashMap<>();

        // Moses
        setTokenizer(Languages.CATALAN, Moses);
        setTokenizer(Languages.CZECH, Moses);
        setTokenizer(Languages.GERMAN, Moses);
        setTokenizer(Languages.GREEK, Moses);
        setTokenizer(Languages.ENGLISH, Moses);
        setTokenizer(Languages.SPANISH, Moses);
        setTokenizer(Languages.FINNISH, Moses);
        setTokenizer(Languages.FRENCH, Moses);
        setTokenizer(Languages.HUNGARIAN, Moses);
        setTokenizer(Languages.ICELANDIC, Moses);
        setTokenizer(Languages.ITALIAN, Moses);
        setTokenizer(Languages.LATVIAN, Moses);
        setTokenizer(Languages.DUTCH, Moses);
        setTokenizer(Languages.POLISH, Moses);
        setTokenizer(Languages.PORTUGUESE, Moses);
        setTokenizer(Languages.ROMANIAN, Moses);
        setTokenizer(Languages.RUSSIAN, Moses);
        setTokenizer(Languages.SLOVAK, Moses);
        setTokenizer(Languages.SLOVENE, Moses);
        setTokenizer(Languages.SWEDISH, Moses);
        setTokenizer(Languages.TAMIL, Moses);

        // CoreNLP
        setTokenizer(Languages.ARABIC, CoreNLP);

        // OpenNLP
        setTokenizer(Languages.DANISH, OpenNLP);
        setTokenizer(Languages.NORTHERN_SAMI, OpenNLP);

        // Lucene
        setTokenizer(Languages.PERSIAN, Lucene);
        setTokenizer(Languages.HINDI, Lucene);
        setTokenizer(Languages.THAI, Lucene);
        setTokenizer(Languages.BULGARIAN, Lucene);
        setTokenizer(Languages.BRAZILIAN, Lucene);
        setTokenizer(Languages.BASQUE, Lucene);
        setTokenizer(Languages.IRISH, Lucene);
        setTokenizer(Languages.ARMENIAN, Lucene);
        setTokenizer(Languages.INDONESIAN, Lucene);
        setTokenizer(Languages.NORWEGIAN, Lucene);
        setTokenizer(Languages.TURKISH, Lucene);

        // Language Tool
        setTokenizer(Languages.BRETON, LanguageTool);
        setTokenizer(Languages.ESPERANTO, LanguageTool);
        setTokenizer(Languages.KHMER, LanguageTool);
        setTokenizer(Languages.MALAYALAM, LanguageTool);
        setTokenizer(Languages.UKRAINIAN, LanguageTool);
        setTokenizer(Languages.GALICIAN, LanguageTool);
        setTokenizer(Languages.TAGALOG, LanguageTool);

        // Customs
        setTokenizer(Languages.HEBREW, HebMorph);
        setTokenizer(Languages.JAPANESE, Kuromoji);
        setTokenizer(Languages.CHINESE, Paoding);
    }

    public static Tokenizer forLanguage(Locale locale) {
        TokenizerLoader loader = tokenizers.get(locale);
        Tokenizer tokenizer = loader == null ? null : loader.load(locale);

        if (tokenizer == null)
            throw new IllegalArgumentException("Unsupported language: " + locale.getLanguage());

        return tokenizer;
    }

}
