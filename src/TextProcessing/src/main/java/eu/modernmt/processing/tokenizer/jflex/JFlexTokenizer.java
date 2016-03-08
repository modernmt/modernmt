package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.processing.Languages;
import eu.modernmt.processing.tokenizer.TokenizedString;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.tokenizer.MultiInstanceTokenizer;
import eu.modernmt.processing.tokenizer.Tokenizer;
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
public class JFlexTokenizer extends MultiInstanceTokenizer {

    public static final JFlexTokenizer CATALAN = new JFlexTokenizer(CatalanAnnotator.class);
    public static final JFlexTokenizer CZECH = new JFlexTokenizer(CzechAnnotator.class);
    public static final JFlexTokenizer GERMAN = new JFlexTokenizer(GermanAnnotator.class);
    public static final JFlexTokenizer GREEK = new JFlexTokenizer(GreekAnnotator.class);
    public static final JFlexTokenizer ENGLISH = new JFlexTokenizer(EnglishAnnotator.class);
    public static final JFlexTokenizer SPANISH = new JFlexTokenizer(SpanishAnnotator.class);
    public static final JFlexTokenizer FINNISH = new JFlexTokenizer(FinnishAnnotator.class);
    public static final JFlexTokenizer FRENCH = new JFlexTokenizer(FrenchAnnotator.class);
    public static final JFlexTokenizer HUNGARIAN = new JFlexTokenizer(HungarianAnnotator.class);
    public static final JFlexTokenizer ICELANDIC = new JFlexTokenizer(IcelandicAnnotator.class);
    public static final JFlexTokenizer ITALIAN = new JFlexTokenizer(ItalianAnnotator.class);
    public static final JFlexTokenizer LATVIAN = new JFlexTokenizer(LatvianAnnotator.class);
    public static final JFlexTokenizer DUTCH = new JFlexTokenizer(DutchAnnotator.class);
    public static final JFlexTokenizer POLISH = new JFlexTokenizer(PolishAnnotator.class);
    public static final JFlexTokenizer PORTUGUESE = new JFlexTokenizer(PortugueseAnnotator.class);
    public static final JFlexTokenizer ROMANIAN = new JFlexTokenizer(RomanianAnnotator.class);
    public static final JFlexTokenizer RUSSIAN = new JFlexTokenizer(RussianAnnotator.class);
    public static final JFlexTokenizer SLOVAK = new JFlexTokenizer(SlovakAnnotator.class);
    public static final JFlexTokenizer SLOVENE = new JFlexTokenizer(SloveneAnnotator.class);
    public static final JFlexTokenizer SWEDISH = new JFlexTokenizer(SwedishAnnotator.class);
    public static final JFlexTokenizer TAMIL = new JFlexTokenizer(TamilAnnotator.class);

    public static final Map<Locale, Tokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.CATALAN, CATALAN);
        ALL.put(Languages.CZECH, CZECH);
        ALL.put(Languages.GERMAN, GERMAN);
        ALL.put(Languages.GREEK, GREEK);
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.SPANISH, SPANISH);
        ALL.put(Languages.FINNISH, FINNISH);
        ALL.put(Languages.FRENCH, FRENCH);
        ALL.put(Languages.HUNGARIAN, HUNGARIAN);
        ALL.put(Languages.ICELANDIC, ICELANDIC);
        ALL.put(Languages.ITALIAN, ITALIAN);
        ALL.put(Languages.LATVIAN, LATVIAN);
        ALL.put(Languages.DUTCH, DUTCH);
        ALL.put(Languages.POLISH, POLISH);
        ALL.put(Languages.PORTUGUESE, PORTUGUESE);
        ALL.put(Languages.ROMANIAN, ROMANIAN);
        ALL.put(Languages.RUSSIAN, RUSSIAN);
        ALL.put(Languages.SLOVAK, SLOVAK);
        ALL.put(Languages.SLOVENE, SLOVENE);
        ALL.put(Languages.SWEDISH, SWEDISH);
        ALL.put(Languages.TAMIL, TAMIL);
    }

    private static class JFlexTokenizerFactory implements TokenizerFactory {

        private Class<? extends JFlexAnnotator> annotatorClass;

        public JFlexTokenizerFactory(Class<? extends JFlexAnnotator> annotatorClass) {
            this.annotatorClass = annotatorClass;
        }

        @Override
        public Tokenizer newInstance() {
            try {
                JFlexAnnotator annotator = this.annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
                return new JFlexTokenizerImpl(annotator);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + this.annotatorClass.getName(), e);
            }
        }
    }

    protected JFlexTokenizer(Class<? extends JFlexAnnotator> annotatorClass) {
        super(new JFlexTokenizerFactory(annotatorClass));
    }

    private static class JFlexTokenizerImpl implements Tokenizer {

        private JFlexAnnotator annotator;

        public JFlexTokenizerImpl(JFlexAnnotator annotator) {
            this.annotator = annotator;
        }

        @Override
        public TokenizedString call(TokenizedString text) throws ProcessingException {
            AnnotatedString astring = new AnnotatedString(text.string);

            annotator.yyreset(astring.getReader());

            int type;
            while ((type = next(annotator)) != JFlexAnnotator.YYEOF) {
                annotator.annotate(astring, type);
            }

            TokenizerOutputTransformer.transform(text, astring.toTokenArray());
            return text;
        }

        private static int next(JFlexAnnotator annotator) throws ProcessingException {
            try {
                return annotator.next();
            } catch (IOException e) {
                throw new ProcessingException(e);
            }
        }

        @Override
        public void close() throws IOException {

        }
    }
}
