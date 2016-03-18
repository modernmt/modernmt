package eu.modernmt.processing.tokenizer.languagetool;

import eu.modernmt.processing.Languages;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.tokenizer.MultiInstanceTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import org.languagetool.language.tokenizers.TagalogWordTokenizer;
import org.languagetool.tokenizers.br.BretonWordTokenizer;
import org.languagetool.tokenizers.eo.EsperantoWordTokenizer;
import org.languagetool.tokenizers.gl.GalicianWordTokenizer;
import org.languagetool.tokenizers.km.KhmerWordTokenizer;
import org.languagetool.tokenizers.ml.MalayalamWordTokenizer;
import org.languagetool.tokenizers.uk.UkrainianWordTokenizer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 12/11/15.
 */
public class LanguageToolTokenizer extends MultiInstanceTokenizer {

    private static class LanguageToolFactory implements TokenizerFactory {

        private Class<? extends org.languagetool.tokenizers.Tokenizer> tokenizerClass;

        public LanguageToolFactory(Class<? extends org.languagetool.tokenizers.Tokenizer> tokenizerClass) {
            this.tokenizerClass = tokenizerClass;
        }

        @Override
        public Tokenizer newInstance() {
            try {
                return new TokenizerImplementation(tokenizerClass.newInstance());
            } catch (IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + this.tokenizerClass.getName(), e);
            }
        }
    }

    public static final LanguageToolTokenizer BRETON = new LanguageToolTokenizer(BretonWordTokenizer.class);
    public static final LanguageToolTokenizer ESPERANTO = new LanguageToolTokenizer(EsperantoWordTokenizer.class);
    public static final LanguageToolTokenizer GALICIAN = new LanguageToolTokenizer(GalicianWordTokenizer.class);
    public static final LanguageToolTokenizer KHMER = new LanguageToolTokenizer(KhmerWordTokenizer.class);
    public static final LanguageToolTokenizer MALAYALAM = new LanguageToolTokenizer(MalayalamWordTokenizer.class);
    public static final LanguageToolTokenizer UKRAINIAN = new LanguageToolTokenizer(UkrainianWordTokenizer.class);
    public static final LanguageToolTokenizer TAGALOG = new LanguageToolTokenizer(TagalogWordTokenizer.class);

    /* Excluded tokenizers */
//    public static final LanguageToolTokenizer SPANISH = new LanguageToolTokenizer(SpanishWordTokenizer.class);
//    public static final LanguageToolTokenizer CATALAN = new LanguageToolTokenizer(CatalanWordTokenizer.class);
//    public static final LanguageToolTokenizer GREEK = new LanguageToolTokenizer(GreekWordTokenizer.class);
//    public static final LanguageToolTokenizer ENGLISH = new LanguageToolTokenizer(EnglishWordTokenizer.class);
//    public static final LanguageToolTokenizer JAPANESE = new LanguageToolTokenizer(JapaneseWordTokenizer.class);
//    public static final LanguageToolTokenizer DUTCH = new LanguageToolTokenizer(DutchWordTokenizer.class);
//    public static final LanguageToolTokenizer POLISH = new LanguageToolTokenizer(PolishWordTokenizer.class);
//    public static final LanguageToolTokenizer ROMANIAN = new LanguageToolTokenizer(RomanianWordTokenizer.class);

    public static final Map<Locale, Tokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.BRETON, BRETON);
        ALL.put(Languages.ESPERANTO, ESPERANTO);
        ALL.put(Languages.GALICIAN, GALICIAN);
        ALL.put(Languages.KHMER, KHMER);
        ALL.put(Languages.MALAYALAM, MALAYALAM);
        ALL.put(Languages.UKRAINIAN, UKRAINIAN);
        ALL.put(Languages.TAGALOG, TAGALOG);

        /* Excluded tokenizers */
//        ALL.put(Languages.CATALAN, CATALAN);
//        ALL.put(Languages.GREEK, GREEK);
//        ALL.put(Languages.ENGLISH, ENGLISH);
//        ALL.put(Languages.SPANISH, SPANISH);
//        ALL.put(Languages.JAPANESE, JAPANESE);
//        ALL.put(Languages.DUTCH, DUTCH);
//        ALL.put(Languages.POLISH, POLISH);
//        ALL.put(Languages.ROMANIAN, ROMANIAN);
    }

    private LanguageToolTokenizer(Class<? extends org.languagetool.tokenizers.Tokenizer> tokenizerClass) {
        super(new LanguageToolFactory(tokenizerClass));
    }

    private static class TokenizerImplementation implements Tokenizer {

        private org.languagetool.tokenizers.Tokenizer tokenizer;

        public TokenizerImplementation(org.languagetool.tokenizers.Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
        }

        @Override
        public TokenizedString call(TokenizedString text) throws ProcessingException {
            List<String> tokens = tokenizer.tokenize(text.string);
            ArrayList<String> result = new ArrayList<>(tokens.size());

            result.addAll(tokens.stream().filter(token -> !token.trim().isEmpty()).collect(Collectors.toList()));

            TokenizerOutputTransformer.transform(text, result);
            return text;
        }

        @Override
        public void close() {
        }
    }

}
