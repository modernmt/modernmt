package eu.modernmt.processing.tokenizer.jflex;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.tokenizer.MultiInstanceTokenizer;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.regex.Pattern;

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

    private static final Pattern UTF8_CONTROL = Pattern.compile("[\\000-\\037]");
    private static final Pattern UTF8_SPACES = Pattern.compile("[\\s\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u202F\\u205F\\u3000]+");

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
        public String[] call(String text) throws ProcessingException {
            text = UTF8_SPACES.matcher(text).replaceAll(" ");
            text = UTF8_CONTROL.matcher(text).replaceAll("");
            text = ' ' + text + ' ';

            char[] chars = text.toCharArray();

            // Mark splitting
            boolean[] splits = new boolean[text.length() + 1];

            for (int i = 0; i < chars.length; i++) {
                int c = chars[i];
                if (c != '-' && !Character.isLetterOrDigit(c)) {
                    splits[i] = splits[i + 1] = true;
                }
            }
            splits[splits.length - 1] = true;

            // Annotate protected patterns
            boolean[] protectedArray = new boolean[text.length() + 1];
            annotator.yyreset(new StringReader(text));

            int type;
            while ((type = next(annotator)) != JFlexAnnotator.YYEOF) {
                annotator.annotate(protectedArray, type);
            }

            // Create tokens
            ArrayList<String> tokens = new ArrayList<>();
            int start = 0;

            for (int i = 1; i < splits.length; i++) {
                if (!protectedArray[i] && splits[i]) {
                    if (!isEmpty(chars, start, i - start))
                        tokens.add(new String(chars, start, i - start).trim());
                    start = i;
                }
            }

            return tokens.toArray(new String[tokens.size()]);
        }

        private static int next(JFlexAnnotator annotator) throws ProcessingException {
            try {
                return annotator.next();
            } catch (IOException e) {
                throw new ProcessingException(e);
            }
        }

        private static boolean isEmpty(char[] chars, int offset, int count) {
            for (int i = 0; i < count; i++) {
                if (chars[offset + i] != ' ')
                    return false;
            }

            return true;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
