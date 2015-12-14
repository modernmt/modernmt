package eu.modernmt.tokenizer.languagetool;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import eu.modernmt.tokenizer.Languages;
import org.languagetool.language.tokenizers.TagalogWordTokenizer;
import org.languagetool.tokenizers.Tokenizer;
import org.languagetool.tokenizers.br.BretonWordTokenizer;
import org.languagetool.tokenizers.ca.CatalanWordTokenizer;
import org.languagetool.tokenizers.el.GreekWordTokenizer;
import org.languagetool.tokenizers.en.EnglishWordTokenizer;
import org.languagetool.tokenizers.eo.EsperantoWordTokenizer;
import org.languagetool.tokenizers.es.SpanishWordTokenizer;
import org.languagetool.tokenizers.gl.GalicianWordTokenizer;
import org.languagetool.tokenizers.ja.JapaneseWordTokenizer;
import org.languagetool.tokenizers.km.KhmerWordTokenizer;
import org.languagetool.tokenizers.ml.MalayalamWordTokenizer;
import org.languagetool.tokenizers.nl.DutchWordTokenizer;
import org.languagetool.tokenizers.pl.PolishWordTokenizer;
import org.languagetool.tokenizers.ro.RomanianWordTokenizer;
import org.languagetool.tokenizers.uk.UkrainianWordTokenizer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 12/11/15.
 */
public class LanguageToolTokenizer extends ITokenizer {

    public static final ITokenizerFactory BRETON = new LanguageToolTokenizerFactory(BretonWordTokenizer.class);
    public static final ITokenizerFactory CATALAN = new LanguageToolTokenizerFactory(CatalanWordTokenizer.class);
    public static final ITokenizerFactory GREEK = new LanguageToolTokenizerFactory(GreekWordTokenizer.class);
    public static final ITokenizerFactory ENGLISH = new LanguageToolTokenizerFactory(EnglishWordTokenizer.class);
    public static final ITokenizerFactory ESPERANTO = new LanguageToolTokenizerFactory(EsperantoWordTokenizer.class);
    public static final ITokenizerFactory SPANISH = new LanguageToolTokenizerFactory(SpanishWordTokenizer.class);
    public static final ITokenizerFactory GALICIAN = new LanguageToolTokenizerFactory(GalicianWordTokenizer.class);
    public static final ITokenizerFactory JAPANESE = new LanguageToolTokenizerFactory(JapaneseWordTokenizer.class);
    public static final ITokenizerFactory KHMER = new LanguageToolTokenizerFactory(KhmerWordTokenizer.class);
    public static final ITokenizerFactory MALAYALAM = new LanguageToolTokenizerFactory(MalayalamWordTokenizer.class);
    public static final ITokenizerFactory DUTCH = new LanguageToolTokenizerFactory(DutchWordTokenizer.class);
    public static final ITokenizerFactory POLISH = new LanguageToolTokenizerFactory(PolishWordTokenizer.class);
    public static final ITokenizerFactory ROMANIAN = new LanguageToolTokenizerFactory(RomanianWordTokenizer.class);
    public static final ITokenizerFactory UKRAINIAN = new LanguageToolTokenizerFactory(UkrainianWordTokenizer.class);
    public static final ITokenizerFactory TAGALOG = new LanguageToolTokenizerFactory(TagalogWordTokenizer.class);

    public static final Map<Locale, ITokenizerFactory> ALL = new HashMap<>();

    static {
        ALL.put(Languages.BRETON, BRETON);
        ALL.put(Languages.CATALAN, CATALAN);
        ALL.put(Languages.GREEK, GREEK);
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.ESPERANTO, ESPERANTO);
        ALL.put(Languages.SPANISH, SPANISH);
        ALL.put(Languages.GALICIAN, GALICIAN);
        ALL.put(Languages.JAPANESE, JAPANESE);
        ALL.put(Languages.KHMER, KHMER);
        ALL.put(Languages.MALAYALAM, MALAYALAM);
        ALL.put(Languages.DUTCH, DUTCH);
        ALL.put(Languages.POLISH, POLISH);
        ALL.put(Languages.ROMANIAN, ROMANIAN);
        ALL.put(Languages.UKRAINIAN, UKRAINIAN);
        ALL.put(Languages.TAGALOG, TAGALOG);
    }

    private Tokenizer tokenizer;

    protected LanguageToolTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public String[] tokenize(String text) {
        List<String> tokens = tokenizer.tokenize(text);
        ArrayList<String> result = new ArrayList<>(tokens.size());

        result.addAll(tokens.stream().filter(token -> !token.trim().isEmpty()).collect(Collectors.toList()));

        return result.toArray(new String[result.size()]);
    }
}
