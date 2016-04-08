package eu.modernmt.processing.tokenizer.corenlp;

import edu.stanford.nlp.international.arabic.process.ArabicTokenizer;
import edu.stanford.nlp.international.french.process.FrenchTokenizer;
import edu.stanford.nlp.international.spanish.process.SpanishTokenizer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.string.XMLEditableString;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 11/11/15.
 */
public class CoreNLPTokenizer implements Tokenizer {

    public static final Tokenizer ENGLISH = new CoreNLPTokenizer(PTBTokenizer.factory(), "ptb3Escaping=false,asciiQuotes=true,normalizeSpace=false");
    public static final Tokenizer ARABIC = new CoreNLPTokenizer(ArabicTokenizer.factory());
    public static final Tokenizer FRENCH = new CoreNLPTokenizer(FrenchTokenizer.factory());
    public static final Tokenizer SPANISH = new CoreNLPTokenizer(SpanishTokenizer.factory());

    public static final Map<Locale, Tokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.ARABIC, ARABIC);
        ALL.put(Languages.FRENCH, FRENCH);
        ALL.put(Languages.SPANISH, SPANISH);
    }

    private TokenizerFactory<?> factory;

    private CoreNLPTokenizer(TokenizerFactory<?> factory) {
        this(factory, null);
    }

    private CoreNLPTokenizer(TokenizerFactory<?> factory, String options) {
        this.factory = factory;
        if (options != null)
            factory.setOptions(options);
    }

    @Override
    public XMLEditableString call(XMLEditableString text, Map<String, Object> metadata) throws ProcessingException {
        Reader reader = new StringReader(text.toString());
        edu.stanford.nlp.process.Tokenizer<?> tokenizer;
        synchronized (this) {
            tokenizer = this.factory.getTokenizer(reader);
        }

        ArrayList<String> result = new ArrayList<String>();

        Boolean hasWord = null;
        while (tokenizer.hasNext()) {
            Object token = tokenizer.next();

            if (hasWord == null)
                hasWord = token instanceof HasWord;

            String word = hasWord ? ((HasWord) token).word() : token.toString();
            result.add(word);
        }

        return TokenizerOutputTransformer.transform(text, result);
    }

    @Override
    public void close() {
    }

}
