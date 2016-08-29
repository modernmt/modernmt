package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.model.Languages;
import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.XMLEditableString;
import eu.modernmt.processing.tokenizer.TokenizerOutputTransformer;
import eu.modernmt.processing.tokenizer.jflex.annotators.*;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 29/01/16.
 */
public class JFlexTokenizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    private static final Map<Locale, Class<? extends JFlexTokenAnnotator>> ANNOTATORS = new HashMap<>();

    static {
        ANNOTATORS.put(Languages.CATALAN, CatalanTokenAnnotator.class);
        ANNOTATORS.put(Languages.CZECH, CzechTokenAnnotator.class);
        ANNOTATORS.put(Languages.GERMAN, GermanTokenAnnotator.class);
        ANNOTATORS.put(Languages.GREEK, GreekTokenAnnotator.class);
        ANNOTATORS.put(Languages.ENGLISH, EnglishTokenAnnotator.class);
        ANNOTATORS.put(Languages.SPANISH, SpanishTokenAnnotator.class);
        ANNOTATORS.put(Languages.FINNISH, FinnishTokenAnnotator.class);
        ANNOTATORS.put(Languages.FRENCH, FrenchTokenAnnotator.class);
        ANNOTATORS.put(Languages.HUNGARIAN, HungarianTokenAnnotator.class);
        ANNOTATORS.put(Languages.ICELANDIC, IcelandicTokenAnnotator.class);
        ANNOTATORS.put(Languages.ITALIAN, ItalianTokenAnnotator.class);
        ANNOTATORS.put(Languages.LATVIAN, LatvianTokenAnnotator.class);
        ANNOTATORS.put(Languages.DUTCH, DutchTokenAnnotator.class);
        ANNOTATORS.put(Languages.POLISH, PolishTokenAnnotator.class);
        ANNOTATORS.put(Languages.PORTUGUESE, PortugueseTokenAnnotator.class);
        ANNOTATORS.put(Languages.ROMANIAN, RomanianTokenAnnotator.class);
        ANNOTATORS.put(Languages.RUSSIAN, RussianTokenAnnotator.class);
        ANNOTATORS.put(Languages.SLOVAK, SlovakTokenAnnotator.class);
        ANNOTATORS.put(Languages.SLOVENE, SloveneTokenAnnotator.class);
        ANNOTATORS.put(Languages.SWEDISH, SwedishTokenAnnotator.class);
        ANNOTATORS.put(Languages.TAMIL, TamilTokenAnnotator.class);
    }

    private final JFlexTokenAnnotator annotator;

    public JFlexTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        Class<? extends JFlexTokenAnnotator> annotatorClass = ANNOTATORS.get(sourceLanguage);

        if (annotatorClass == null) {
            this.annotator = new StandardTokenAnnotator((Reader) null);
        } else {
            try {
                this.annotator = annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + annotatorClass.getName(), e);
            }
        }
    }

    @Override
    public XMLEditableString call(XMLEditableString text, Map<String, Object> metadata) throws ProcessingException {
        TokensAnnotatedString astring = new TokensAnnotatedString(text.toString());

        annotator.yyreset(astring.getReader());

        int type;
        while ((type = next(annotator)) != JFlexTokenAnnotator.YYEOF) {
            annotator.annotate(astring, type);
        }

        return TokenizerOutputTransformer.transform(text, astring.toTokenArray());
    }

    private static int next(JFlexTokenAnnotator annotator) throws ProcessingException {
        try {
            return annotator.next();
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

}
