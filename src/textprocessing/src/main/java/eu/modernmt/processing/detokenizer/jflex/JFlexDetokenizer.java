package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.lang.Language2;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.detokenizer.jflex.annotators.*;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 29/01/16.
 */
public class JFlexDetokenizer extends TextProcessor<Translation, Translation> {

    private static final Map<String, Class<? extends JFlexSpaceAnnotator>> ANNOTATORS = new HashMap<>();

    static {
        ANNOTATORS.put(Language2.CATALAN.getLanguage(), CatalanSpaceAnnotator.class);
        ANNOTATORS.put(Language2.ENGLISH.getLanguage(), EnglishSpaceAnnotator.class);
        ANNOTATORS.put(Language2.ITALIAN.getLanguage(), ItalianSpaceAnnotator.class);
        ANNOTATORS.put(Language2.GERMAN.getLanguage(), GermanSpaceAnnotator.class);
        ANNOTATORS.put(Language2.FRENCH.getLanguage(), FrenchSpaceAnnotator.class);
        ANNOTATORS.put(Language2.THAI.getLanguage(), ThaiSpaceAnnotator.class);
    }

    private final JFlexSpaceAnnotator annotator;

    public static JFlexSpaceAnnotator newAnnotator(Language2 language) {
        Class<? extends JFlexSpaceAnnotator> annotatorClass = ANNOTATORS.get(language.getLanguage());
        if (annotatorClass == null) {
            return new StandardSpaceAnnotator((Reader) null);
        } else {
            try {
                return annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + annotatorClass.getName(), e);
            }
        }
    }

    public JFlexDetokenizer(Language2 sourceLanguage, Language2 targetLanguage) {
        super(sourceLanguage, targetLanguage);
        this.annotator = getAnnotator(targetLanguage);
    }

    protected JFlexSpaceAnnotator getAnnotator(Language2 language) {
        return newAnnotator(language);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        SpacesAnnotatedString text = SpacesAnnotatedString.fromSentence(translation);

        annotator.reset(text.getReader());

        int type;
        while ((type = next(annotator)) != JFlexSpaceAnnotator.YYEOF) {
            annotator.annotate(text, type);
        }

        text.apply(translation, (word, hasSpace) -> {
            if (word.hasRightSpace())
                word.setRightSpace(hasSpace ? " " : null);
        });

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
