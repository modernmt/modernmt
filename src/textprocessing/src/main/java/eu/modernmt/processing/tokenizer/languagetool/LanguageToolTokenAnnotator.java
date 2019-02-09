package eu.modernmt.processing.tokenizer.languagetool;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;
import org.languagetool.language.tokenizers.TagalogWordTokenizer;
import org.languagetool.tokenizers.br.BretonWordTokenizer;
import org.languagetool.tokenizers.eo.EsperantoWordTokenizer;
import org.languagetool.tokenizers.gl.GalicianWordTokenizer;
import org.languagetool.tokenizers.km.KhmerWordTokenizer;
import org.languagetool.tokenizers.ml.MalayalamWordTokenizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageToolTokenAnnotator implements BaseTokenizer.Annotator {

    private static final Map<Language, Class<? extends org.languagetool.tokenizers.Tokenizer>> TOKENIZERS = new HashMap<>();

    static {
        TOKENIZERS.put(Language.BRETON, BretonWordTokenizer.class);
        TOKENIZERS.put(Language.ESPERANTO, EsperantoWordTokenizer.class);
        TOKENIZERS.put(Language.GALICIAN, GalicianWordTokenizer.class);
        TOKENIZERS.put(Language.KHMER, KhmerWordTokenizer.class);
        TOKENIZERS.put(Language.MALAYALAM, MalayalamWordTokenizer.class);
        TOKENIZERS.put(Language.TAGALOG, TagalogWordTokenizer.class);

        /* Excluded tokenizers */
//        TOKENIZERS.put(Language.CATALAN, CatalanWordTokenizer.class);
//        TOKENIZERS.put(Language.GREEK, GreekWordTokenizer.class);
//        TOKENIZERS.put(Language.ENGLISH, EnglishWordTokenizer.class);
//        TOKENIZERS.put(Language.SPANISH, SpanishWordTokenizer.class);
//        TOKENIZERS.put(Language.JAPANESE, JapaneseWordTokenizer.class);
//        TOKENIZERS.put(Language.DUTCH, DutchWordTokenizer.class);
//        TOKENIZERS.put(Language.POLISH, PolishWordTokenizer.class);
//        TOKENIZERS.put(Language.ROMANIAN, RomanianWordTokenizer.class);
//        TOKENIZERS.put(Language.UKRAINIAN, UkrainianWordTokenizer.class); " 6228.05.55954 "
    }

    private final org.languagetool.tokenizers.Tokenizer tokenizer;

    public static LanguageToolTokenAnnotator forLanguage(Language language) throws UnsupportedLanguageException {
        Class<? extends org.languagetool.tokenizers.Tokenizer> tokenizerClass = TOKENIZERS.get(language);
        if (tokenizerClass == null)
            throw new UnsupportedLanguageException(language);

        try {
            return new LanguageToolTokenAnnotator(tokenizerClass.newInstance());
        } catch (IllegalAccessException | InstantiationException e) {
            throw new Error("Error during class instantiation: " + tokenizerClass.getName(), e);
        }
    }

    private LanguageToolTokenAnnotator(org.languagetool.tokenizers.Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public void annotate(TokenizedString string) throws ProcessingException {
        List<String> tokens = tokenizer.tokenize(string.toString());
        tokens.removeIf(token -> token.trim().isEmpty());

        annotate(string, tokens);
    }

    public static void annotate(TokenizedString tokenizedString, List<String> tokens) throws ProcessingException {
        String string = tokenizedString.toString();
        int length = string.length();

        int stringIndex = 0;

        for (String token : tokens) {
            int tokenPos = string.indexOf(token, stringIndex);

            if (tokenPos < 0)
                throw new ProcessingException("Unable to find token '" + token + "' starting from index " +
                        stringIndex + " in sentence \"" + tokenizedString + "\"");

            int tokenLength = token.length();

            stringIndex = tokenPos + tokenLength;
            if (stringIndex <= length)
                tokenizedString.setWord(tokenPos, tokenPos + tokenLength);
        }
    }
}
