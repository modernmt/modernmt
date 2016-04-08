package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.processing.Languages;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.string.XMLEditableString;
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

    public static final JFlexTokenizer CATALAN = new JFlexTokenizer(CatalanTokenAnnotator.class);
    public static final JFlexTokenizer CZECH = new JFlexTokenizer(CzechTokenAnnotator.class);
    public static final JFlexTokenizer GERMAN = new JFlexTokenizer(GermanTokenAnnotator.class);
    public static final JFlexTokenizer GREEK = new JFlexTokenizer(GreekTokenAnnotator.class);
    public static final JFlexTokenizer ENGLISH = new JFlexTokenizer(EnglishTokenAnnotator.class);
    public static final JFlexTokenizer SPANISH = new JFlexTokenizer(SpanishTokenAnnotator.class);
    public static final JFlexTokenizer FINNISH = new JFlexTokenizer(FinnishTokenAnnotator.class);
    public static final JFlexTokenizer FRENCH = new JFlexTokenizer(FrenchTokenAnnotator.class);
    public static final JFlexTokenizer HUNGARIAN = new JFlexTokenizer(HungarianTokenAnnotator.class);
    public static final JFlexTokenizer ICELANDIC = new JFlexTokenizer(IcelandicTokenAnnotator.class);
    public static final JFlexTokenizer ITALIAN = new JFlexTokenizer(ItalianTokenAnnotator.class);
    public static final JFlexTokenizer LATVIAN = new JFlexTokenizer(LatvianTokenAnnotator.class);
    public static final JFlexTokenizer DUTCH = new JFlexTokenizer(DutchTokenAnnotator.class);
    public static final JFlexTokenizer POLISH = new JFlexTokenizer(PolishTokenAnnotator.class);
    public static final JFlexTokenizer PORTUGUESE = new JFlexTokenizer(PortugueseTokenAnnotator.class);
    public static final JFlexTokenizer ROMANIAN = new JFlexTokenizer(RomanianTokenAnnotator.class);
    public static final JFlexTokenizer RUSSIAN = new JFlexTokenizer(RussianTokenAnnotator.class);
    public static final JFlexTokenizer SLOVAK = new JFlexTokenizer(SlovakTokenAnnotator.class);
    public static final JFlexTokenizer SLOVENE = new JFlexTokenizer(SloveneTokenAnnotator.class);
    public static final JFlexTokenizer SWEDISH = new JFlexTokenizer(SwedishTokenAnnotator.class);
    public static final JFlexTokenizer TAMIL = new JFlexTokenizer(TamilTokenAnnotator.class);

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

        private Class<? extends JFlexTokenAnnotator> annotatorClass;

        public JFlexTokenizerFactory(Class<? extends JFlexTokenAnnotator> annotatorClass) {
            this.annotatorClass = annotatorClass;
        }

        @Override
        public Tokenizer newInstance() {
            try {
                JFlexTokenAnnotator annotator = this.annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
                return new JFlexTokenizerImpl(annotator);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + this.annotatorClass.getName(), e);
            }
        }
    }

    protected JFlexTokenizer(Class<? extends JFlexTokenAnnotator> annotatorClass) {
        super(new JFlexTokenizerFactory(annotatorClass));
    }

    private static class JFlexTokenizerImpl implements Tokenizer {

        private JFlexTokenAnnotator annotator;

        public JFlexTokenizerImpl(JFlexTokenAnnotator annotator) {
            this.annotator = annotator;
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

        @Override
        public void close() throws IOException {

        }
    }
}
