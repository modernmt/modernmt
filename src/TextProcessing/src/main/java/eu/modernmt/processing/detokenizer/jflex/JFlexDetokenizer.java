package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.MultiInstanceDetokenizer;
import eu.modernmt.processing.detokenizer.jflex.annotators.EnglishSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.FrenchSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.ItalianSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.StandardSpaceAnnotator;
import eu.modernmt.processing.framework.ProcessingException;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 29/01/16.
 */
public class JFlexDetokenizer extends MultiInstanceDetokenizer {

    public static final JFlexDetokenizer DEFAULT = new JFlexDetokenizer(StandardSpaceAnnotator.class);
    public static final JFlexDetokenizer ENGLISH = new JFlexDetokenizer(EnglishSpaceAnnotator.class);
    public static final JFlexDetokenizer ITALIAN = new JFlexDetokenizer(ItalianSpaceAnnotator.class);
    public static final JFlexDetokenizer FRENCH = new JFlexDetokenizer(FrenchSpaceAnnotator.class);

    public static final Map<Locale, JFlexDetokenizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.ITALIAN, ITALIAN);
        ALL.put(Languages.FRENCH, FRENCH);
    }

    private static class JFlexDetokenizerFactory implements DetokenizerFactory {

        private Class<? extends JFlexSpaceAnnotator> annotatorClass;

        public JFlexDetokenizerFactory(Class<? extends JFlexSpaceAnnotator> annotatorClass) {
            this.annotatorClass = annotatorClass;
        }

        @Override
        public Detokenizer newInstance() {
            try {
                JFlexSpaceAnnotator annotator = this.annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
                return new JFlexDetokenizerImpl(annotator);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + this.annotatorClass.getName(), e);
            }
        }

    }

    public JFlexDetokenizer(Class<? extends JFlexSpaceAnnotator> annotatorClass) {
        super(new JFlexDetokenizerFactory(annotatorClass));
    }

    private static class JFlexDetokenizerImpl implements Detokenizer {

        private final JFlexSpaceAnnotator annotator;

        public JFlexDetokenizerImpl(JFlexSpaceAnnotator annotator) {
            this.annotator = annotator;
        }

        @Override
        public Translation call(Translation translation) throws ProcessingException {
            SpacesAnnotatedString text = SpacesAnnotatedString.fromTranslation(translation);

            annotator.reset(text.getReader());

            int type;
            while ((type = next(annotator)) != JFlexSpaceAnnotator.YYEOF) {
                annotator.annotate(text, type);
            }

            text.apply(translation);
            return translation;
        }

        private static int next(JFlexSpaceAnnotator annotator) throws ProcessingException {
            try {
                return annotator.next();
            } catch (IOException e) {
                throw new ProcessingException(e);
            }
        }

        @Override
        public void close() {
            // Nothing to do
        }
    }

}
