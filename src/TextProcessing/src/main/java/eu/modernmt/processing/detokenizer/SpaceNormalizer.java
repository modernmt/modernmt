package eu.modernmt.processing.detokenizer;

import eu.modernmt.processing.Languages;
import eu.modernmt.processing.detokenizer.jflex.JFlexSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.SpacesAnnotatedString;
import eu.modernmt.processing.detokenizer.jflex.annotators.EnglishSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.FrenchSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.ItalianSpaceAnnotator;
import eu.modernmt.processing.detokenizer.jflex.annotators.StandardSpaceAnnotator;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.ProcessedString;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by davide on 30/03/16.
 */
public class SpaceNormalizer implements TextProcessor<ProcessedString, ProcessedString> {

    public static final SpaceNormalizer DEFAULT = new SpaceNormalizer(StandardSpaceAnnotator.class);
    public static final SpaceNormalizer ENGLISH = new SpaceNormalizer(EnglishSpaceAnnotator.class);
    public static final SpaceNormalizer ITALIAN = new SpaceNormalizer(ItalianSpaceAnnotator.class);
    public static final SpaceNormalizer FRENCH = new SpaceNormalizer(FrenchSpaceAnnotator.class);

    public static final Map<Locale, SpaceNormalizer> ALL = new HashMap<>();

    static {
        ALL.put(Languages.ENGLISH, ENGLISH);
        ALL.put(Languages.ITALIAN, ITALIAN);
        ALL.put(Languages.FRENCH, FRENCH);
    }

    public static SpaceNormalizer forLanguage(Locale language) {
        SpaceNormalizer normalizer = SpaceNormalizer.ALL.get(language);
        return normalizer == null ? SpaceNormalizer.DEFAULT : normalizer;
    }

    private Class<? extends JFlexSpaceAnnotator> annotatorClass;
    private Queue<JFlexSpaceAnnotator> buffer = new ConcurrentLinkedQueue<>();
    private boolean closed = false;

    private SpaceNormalizer(Class<? extends JFlexSpaceAnnotator> annotatorClass) {
        this.annotatorClass = annotatorClass;
    }

    private JFlexSpaceAnnotator getInstance() {
        JFlexSpaceAnnotator instance = buffer.poll();

        if (instance == null) {
            try {
                instance = this.annotatorClass.getConstructor(Reader.class).newInstance((Reader) null);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new Error("Error during class instantiation: " + this.annotatorClass.getName(), e);
            }
        }

        return instance;
    }

    private void releaseInstance(JFlexSpaceAnnotator annotator) {
        synchronized (this) {
            if (!closed)
                buffer.add(annotator);
        }
    }

    @Override
    public ProcessedString call(ProcessedString string) throws ProcessingException {
        JFlexSpaceAnnotator annotator = getInstance();

        try {
            SpacesAnnotatedString text = SpacesAnnotatedString.fromString(string.toString());
            annotator.reset(text.getReader());

            int type;
            while ((type = next(annotator)) != JFlexSpaceAnnotator.YYEOF) {
                annotator.annotate(text, type);
            }

            return text.apply(string);
        } finally {
            releaseInstance(annotator);
        }
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
        synchronized (this) {
            closed = true;
        }
    }

}
