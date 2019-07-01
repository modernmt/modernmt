package eu.modernmt.processing.tokenizer.corenlp;

import edu.stanford.nlp.international.arabic.process.ArabicTokenizer;
import edu.stanford.nlp.international.french.process.FrenchTokenizer;
import edu.stanford.nlp.international.spanish.process.SpanishTokenizer;
import edu.stanford.nlp.ling.HasOffset;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.TokenizedString;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class CoreNLPTokenAnnotator implements BaseTokenizer.Annotator {

    private static final Map<Language, TokenizerFactory<?>> FACTORIES = new HashMap<>();

    static {
        FACTORIES.put(Language.ENGLISH, PTBTokenizer.factory());
        FACTORIES.put(Language.ARABIC, ArabicTokenizer.factory());
        FACTORIES.put(Language.FRENCH, FrenchTokenizer.factory());
        FACTORIES.put(Language.SPANISH, SpanishTokenizer.factory());
    }

    private final TokenizerFactory<?> factory;

    public static CoreNLPTokenAnnotator forLanguage(Language language) throws UnsupportedLanguageException {
        TokenizerFactory<?> factory = FACTORIES.get(language);
        if (factory == null)
            throw new UnsupportedLanguageException(language);

        /*sets special options if source language is English*/
        if (Language.ENGLISH.getLanguage().equals(language.getLanguage()))
            factory.setOptions("ptb3Escaping=false,asciiQuotes=true,normalizeSpace=false");

        return new CoreNLPTokenAnnotator(factory);
    }

    private CoreNLPTokenAnnotator(TokenizerFactory<?> factory) {
        this.factory = factory;
    }

    @Override
    public void annotate(TokenizedString string) {
        Reader reader = new StringReader(string.toString());
        edu.stanford.nlp.process.Tokenizer<?> tokenizer;
        synchronized (this) {
            tokenizer = this.factory.getTokenizer(reader);
        }

        while (tokenizer.hasNext()) {
            Object token = tokenizer.next();

            if (token instanceof HasOffset) {
                HasOffset hasOffset = (HasOffset) token;
                int begin = hasOffset.beginPosition();
                int end = hasOffset.endPosition();

                string.setWord(begin, end);
            }
        }
    }

}
