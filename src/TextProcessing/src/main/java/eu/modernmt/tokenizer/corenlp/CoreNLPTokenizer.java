package eu.modernmt.tokenizer.corenlp;

import edu.stanford.nlp.international.arabic.process.ArabicTokenizer;
import edu.stanford.nlp.international.french.process.FrenchTokenizer;
import edu.stanford.nlp.international.spanish.process.SpanishTokenizer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.ITokenizerFactory;
import eu.modernmt.tokenizer.Languages;

import java.io.Reader;
import java.io.StringReader;
import java.util.*;

/**
 * Created by davide on 11/11/15.
 */
public class CoreNLPTokenizer extends ITokenizer {

    public static final ITokenizerFactory ENGLISH = new CoreNLPTokenizerFactory(PTBTokenizer.factory(), "ptb3Escaping=false,asciiQuotes=true,normalizeSpace=false");
    public static final ITokenizerFactory ARABIC = new CoreNLPTokenizerFactory(ArabicTokenizer.factory());
    public static final ITokenizerFactory FRENCH = new CoreNLPTokenizerFactory(FrenchTokenizer.factory());
    public static final ITokenizerFactory SPANISH = new CoreNLPTokenizerFactory(SpanishTokenizer.factory());

    public static final Map<Locale, ITokenizerFactory> ALL = new HashMap<>();

    static {
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.ARABIC, ARABIC);
        ALL.put(Languages.FRENCH, FRENCH);
        ALL.put(Languages.SPANISH, SPANISH);
    }

    private TokenizerFactory<?> factory;

    public CoreNLPTokenizer(TokenizerFactory<?> factory) {
        this.factory = factory;
    }

    @Override
    public String[] tokenize(String text) {
        Reader reader = new StringReader(text);
        Tokenizer<?> tokenizer = this.factory.getTokenizer(reader);

        ArrayList<String> result = new ArrayList<String>();

        Boolean hasWord = null;
        while (tokenizer.hasNext()) {
            Object token = tokenizer.next();

            if (hasWord == null)
                hasWord = token instanceof HasWord;

            String word = hasWord ? ((HasWord) token).word() : token.toString();
            result.add(word);
        }

        return result.toArray(new String[result.size()]);
    }

}
