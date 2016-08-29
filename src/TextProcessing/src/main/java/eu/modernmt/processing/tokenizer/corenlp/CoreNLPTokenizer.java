package eu.modernmt.processing.tokenizer.corenlp;

import edu.stanford.nlp.international.arabic.process.ArabicTokenizer;
import edu.stanford.nlp.international.french.process.FrenchTokenizer;
import edu.stanford.nlp.international.spanish.process.SpanishTokenizer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import eu.modernmt.model.Languages;
import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.XMLEditableString;
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
public class CoreNLPTokenizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    private static final Map<Locale, TokenizerFactory<?>> FACTORIES = new HashMap<>();

    static {
        FACTORIES.put(Languages.ENGLISH, PTBTokenizer.factory());
        FACTORIES.put(Languages.ARABIC, ArabicTokenizer.factory());
        FACTORIES.put(Languages.FRENCH, FrenchTokenizer.factory());
        FACTORIES.put(Languages.SPANISH, SpanishTokenizer.factory());
    }

    private final TokenizerFactory<?> factory;

    public CoreNLPTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        this.factory = FACTORIES.get(sourceLanguage);
        if (this.factory == null)
            throw new LanguageNotSupportedException(sourceLanguage);


        if (Languages.sameLanguage(Languages.ENGLISH, sourceLanguage))
            this.factory.setOptions("ptb3Escaping=false,asciiQuotes=true,normalizeSpace=false");
    }

    @Override
    public XMLEditableString call(XMLEditableString text, Map<String, Object> metadata) throws ProcessingException {
        Reader reader = new StringReader(text.toString());
        edu.stanford.nlp.process.Tokenizer<?> tokenizer;
        synchronized (this) {
            tokenizer = this.factory.getTokenizer(reader);
        }

        ArrayList<String> result = new ArrayList<>();

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

}
