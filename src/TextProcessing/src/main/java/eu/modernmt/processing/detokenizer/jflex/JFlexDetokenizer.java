package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.jflex.annotators.EnglishSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.FrenchSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.ItalianSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.StandardSpaceAnnotator;
import eu.modernmt.processing.framework.LanguageNotSupportedException;
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
public class JFlexDetokenizer extends Detokenizer {

    private static final Map<Locale, Class<? extends JFlexSpaceAnnotator>> ANNOTATORS = new HashMap<>();

    static {
        ANNOTATORS.put(Languages.ENGLISH, EnglishSpaceAnnotator.class);
        ANNOTATORS.put(Languages.ITALIAN, ItalianSpaceAnnotator.class);
        ANNOTATORS.put(Languages.FRENCH, FrenchSpaceAnnotator.class);
    }

    private final JFlexSpaceAnnotator annotator;

    public JFlexDetokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);

        Class<? extends JFlexSpaceAnnotator> annotatorClass = ANNOTATORS.get(targetLanguage);
        if (annotatorClass == null) {
            this.annotator = new StandardSpaceAnnotator((Reader) null);
        } else {
            try {
                this.annotator = annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + annotatorClass.getName(), e);
            }
        }
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
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

}
