package eu.modernmt.processing.tokenizer.languagetool;

import eu.modernmt.model.Languages;
import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.XMLEditableString;
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
public class LanguageToolTokenizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    private static final Map<Locale, Class<? extends org.languagetool.tokenizers.Tokenizer>> TOKENIZERS = new HashMap<>();

    static {
        TOKENIZERS.put(Languages.BRETON, BretonWordTokenizer.class);
        TOKENIZERS.put(Languages.ESPERANTO, EsperantoWordTokenizer.class);
        TOKENIZERS.put(Languages.GALICIAN, GalicianWordTokenizer.class);
        TOKENIZERS.put(Languages.KHMER, KhmerWordTokenizer.class);
        TOKENIZERS.put(Languages.MALAYALAM, MalayalamWordTokenizer.class);
        TOKENIZERS.put(Languages.UKRAINIAN, UkrainianWordTokenizer.class);
        TOKENIZERS.put(Languages.TAGALOG, TagalogWordTokenizer.class);

        /* Excluded tokenizers */
//        TOKENIZERS.put(Languages.CATALAN, CatalanWordTokenizer.class);
//        TOKENIZERS.put(Languages.GREEK, GreekWordTokenizer.class);
//        TOKENIZERS.put(Languages.ENGLISH, EnglishWordTokenizer.class);
//        TOKENIZERS.put(Languages.SPANISH, SpanishWordTokenizer.class);
//        TOKENIZERS.put(Languages.JAPANESE, JapaneseWordTokenizer.class);
//        TOKENIZERS.put(Languages.DUTCH, DutchWordTokenizer.class);
//        TOKENIZERS.put(Languages.POLISH, PolishWordTokenizer.class);
//        TOKENIZERS.put(Languages.ROMANIAN, RomanianWordTokenizer.class);
    }

    private org.languagetool.tokenizers.Tokenizer tokenizer;

    public LanguageToolTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        Class<? extends org.languagetool.tokenizers.Tokenizer> tokenizerClass = TOKENIZERS.get(sourceLanguage);
        if (tokenizerClass == null)
            throw new LanguageNotSupportedException(sourceLanguage);

        try {
            this.tokenizer = tokenizerClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new Error("Error during class instantiation: " + tokenizerClass.getName(), e);
        }
    }

    @Override
    public XMLEditableString call(XMLEditableString text, Map<String, Object> metadata) throws ProcessingException {
        List<String> tokens = tokenizer.tokenize(text.toString());
        ArrayList<String> result = new ArrayList<>(tokens.size());

        result.addAll(tokens.stream().filter(token -> !token.trim().isEmpty()).collect(Collectors.toList()));

        return TokenizerOutputTransformer.transform(text, result);
    }

}
