package eu.modernmt.tokenizer.opennlp;

import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import eu.modernmt.tokenizer.Languages;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 13/11/15.
 */
public class OpenNLPTokenizer extends ITokenizer {

    public static final ITokenizerFactory DANISH = new OpenNLPTokenizerFactory("da");
    public static final ITokenizerFactory GERMAN = new OpenNLPTokenizerFactory("de");
    public static final ITokenizerFactory ENGLISH = new OpenNLPTokenizerFactory("en");
    public static final ITokenizerFactory ITALIAN = new OpenNLPTokenizerFactory("it");
    public static final ITokenizerFactory DUTCH = new OpenNLPTokenizerFactory("nl");
    public static final ITokenizerFactory PORTUGUESE = new OpenNLPTokenizerFactory("pt");
    public static final ITokenizerFactory NORTHERN_SAMI = new OpenNLPTokenizerFactory("se");

    public static final Map<Locale, ITokenizerFactory> ALL = new HashMap<>();

    static {
        ALL.put(Languages.DANISH, DANISH);
        ALL.put(Languages.GERMAN, GERMAN);
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.ITALIAN, ITALIAN);
        ALL.put(Languages.DUTCH, DUTCH);
        ALL.put(Languages.PORTUGUESE, PORTUGUESE);
        ALL.put(Languages.NORTHERN_SAMI, NORTHERN_SAMI);
    }

    private TokenizerME tokenizer;

    protected OpenNLPTokenizer(TokenizerModel model) {
        this.tokenizer = new TokenizerME(model);
    }

    @Override
    public String[] tokenize(String text) {
        return tokenizer.tokenize(text);
    }
}
