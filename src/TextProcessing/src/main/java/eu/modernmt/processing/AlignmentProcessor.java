package eu.modernmt.processing;

import eu.modernmt.aligner.AlignmentInterpolator;
import eu.modernmt.model.Translation;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 22/07/16.
 */
public class AlignmentProcessor extends TextProcessor<Translation, Translation> {

    public AlignmentProcessor(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) {
        if (translation.hasAlignment()) {
            int numberOfSourceTokens = translation.getSource().getWords().length;
            int numberOfTranslationTokens = translation.getWords().length;
            int[][] interpolatedAlignments = AlignmentInterpolator.interpolate(translation.getAlignment(), numberOfSourceTokens, numberOfTranslationTokens);
            translation.setAlignment(interpolatedAlignments);
        }

        return translation;
    }

}
