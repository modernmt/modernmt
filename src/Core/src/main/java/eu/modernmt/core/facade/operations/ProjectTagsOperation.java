package eu.modernmt.core.facade.operations;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.core.Engine;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.util.TokensOutputter;
import eu.modernmt.processing.xml.XMLTagProjector;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * Created by davide on 22/04/16.
 */
public class ProjectTagsOperation extends Operation<Translation> {

    private static final Logger logger = LogManager.getLogger(ProjectTagsOperation.class);
    private static final XMLTagProjector tagProjector = new XMLTagProjector();
    private static Preprocessor targetPreprocessor = null;

    private String sentenceString;
    private String translationString;
    private final Symmetrisation.Strategy symmetrizationStrategy;
    private final boolean invert;

    public ProjectTagsOperation(String sentence, String translation, Symmetrisation.Strategy symmetrizationStrategy, boolean invert) {
        this.sentenceString = sentence;
        this.translationString = translation;
        this.symmetrizationStrategy = symmetrizationStrategy;
        this.invert = invert;
    }

    @Override
    public Translation call() throws ProcessingException, AlignerException {
        Engine engine = getEngine();
        Aligner aligner = engine.getAligner();
        Preprocessor preprocessor = engine.getPreprocessor();

        if (targetPreprocessor == null) {
            synchronized (ProjectTagsOperation.class) {
                if (targetPreprocessor == null) {
                    targetPreprocessor = new Preprocessor(engine.getTargetLanguage());
                }
            }
        }

        long beginTime = System.currentTimeMillis();
        long endTime;

        String sentenceString = invert ? this.translationString : this.sentenceString;
        String translationString = invert ? this.sentenceString : this.translationString;

        Sentence sentence = preprocessor.process(sentenceString, true);
        Sentence translation = targetPreprocessor.process(translationString, true);

        if (this.symmetrizationStrategy != null)
            aligner.setSymmetrizationStrategy(this.symmetrizationStrategy);

        int[][] alignments = aligner.getAlignments(sentence, translation);

        Translation taggedTranslation = new Translation(translation.getWords(), sentence, alignments);
        tagProjector.call(taggedTranslation, null);

        endTime = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("Total time for tags projection: " + (endTime - beginTime) + " [ms]");
        }

        if (this.invert) {
            taggedTranslation = invertTranslation(taggedTranslation);
        }

        return taggedTranslation;
    }

    private static Translation invertTranslation(Translation translation) {
        return new Translation(
                translation.getSource().getWords(), translation.getTags(),
                translation, translation.getAlignment());
    }

    public static void main(String[] args) throws Throwable {
        Preprocessor sourcePreprocessor = null;
        Preprocessor targetPreprocessor = null;

        try {
            String sentence = "<b><span>To request information on migration, contact  </span></b><b><span><a><span>Migrazione.PostaFA@fiat.com </span></a></span></b>";
            String translation = "<b><span>Per richiesta di informazioni sulla migrazione,fare<b><span><a><span> riferimento a Migrazione.PostaFA</span></b>@fiat.com </span></a></span></b>";
            sourcePreprocessor = new Preprocessor(Locale.forLanguageTag("en"));
            targetPreprocessor = new Preprocessor(Locale.forLanguageTag("it"));
            Sentence preprocessedSentence = sourcePreprocessor.process(sentence, true);
            Sentence preprocessedTranslation = targetPreprocessor.process(translation, true);

            String alignments_str = "[[0, 0], [1, 1], [2, 3], [3, 4], [4, 5], [5, 6], [5, 7], [6, 10], [7, 8], [7, 10], [8, 9], [8, 11], [9, 12]]";
            alignments_str = alignments_str.substring(0, alignments_str.length() - 2).substring(2)
                    .replaceAll("\\], \\[", " ").replaceAll(", ", "-");
            int[][] alignments = FastAlign.parseAlignments(alignments_str);

            System.out.println("Original source:\n" + sentence);
            System.out.println("Tokenized source:\n" + TokensOutputter.toString(preprocessedSentence, false, true));
            System.out.println("\nOriginal translation:\n" + translation);
            System.out.println("Tokenized translation:\n" + TokensOutputter.toString(preprocessedTranslation, false, true));
            System.out.println("\nAlignments:\n" + alignments_str);

            Translation taggedTranslation = new Translation(
                    preprocessedTranslation.getWords(), preprocessedSentence, alignments);

            tagProjector.call(taggedTranslation, null);

            System.out.println("\nTagged translation:\n" + taggedTranslation);
        } finally {
            IOUtils.closeQuietly(sourcePreprocessor);
            IOUtils.closeQuietly(targetPreprocessor);
        }
    }

}
