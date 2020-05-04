package eu.modernmt.cleaning.filters.lang;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.*;
import eu.modernmt.cleaning.CorpusFilter;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.Language;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

abstract class AbstractOptimaizeFilter implements CorpusFilter {

    private static final HashSet<String> SUPPORTED_LANGUAGES = new HashSet<>(
            Arrays.asList("af", "an", "ar", "ast", "be", "br", "ca", "bg", "bn", "cs", "cy", "da", "de", "el", "en",
                    "es", "et", "eu", "fa", "fi", "fr", "ga", "gl", "gu", "he", "hi", "hr", "ht", "hu", "id", "is",
                    "it", "ja", "km", "kn", "ko", "lt", "lv", "mk", "ml", "mr", "ms", "mt", "ne", "nl", "no", "oc",
                    "pa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl",
                    "tr", "uk", "ur", "vi", "wa", "yi", "zh"));

    private static LanguageDetector detectorInstance = null;

    protected final LanguageDetector getLanguageDetector() {
        if (detectorInstance == null) {
            synchronized (AbstractOptimaizeFilter.class) {
                if (detectorInstance == null) {
                    try {
                        detectorInstance = LanguageDetectorBuilder.create(NgramExtractors.standard())
                                .shortTextAlgorithm(0)
                                .withProfiles(new LanguageProfileReader().readAllBuiltIn())
                                .build();
                    } catch (IOException e) {
                        throw new RuntimeIOException(e);
                    }
                }
            }
        }

        return detectorInstance;
    }

    protected final boolean isSupported(Language language) {
        return SUPPORTED_LANGUAGES.contains(language.getLanguage());
    }

    protected final String makeLanguageKey(String language) {
        // We cannot rely on identification of these languages
        if ("sr".equalsIgnoreCase(language) || "hr".equalsIgnoreCase(language) || "bs".equalsIgnoreCase(language)) {
            return "sr_hr_bs";
        } else if ("id".equalsIgnoreCase(language) || "ms".equalsIgnoreCase(language)) {
            return "id_ms";
        } else {
            return language.toLowerCase();
        }
    }

    protected String guessLanguage(CharSequence text, boolean largeText) {
        return guessLanguage(text, largeText, .50f);
    }

    protected String guessLanguage(CharSequence text, boolean largeText, float minProbability) {
        LanguageDetector detector = getLanguageDetector();

        TextObjectFactory factory;
        if (largeText) {
            factory = CommonTextObjectFactories.forDetectingOnLargeText();
        } else {
            factory = new TextObjectFactoryBuilder()
                    .withTextFilter(UrlTextFilter.getInstance())
                    .build();
        }

        TextObject textObject = factory.create().append(text);
        List<DetectedLanguage> languages = detector.getProbabilities(textObject);

        if (languages.size() < 1)
            return null;

        DetectedLanguage lang = languages.get(0);
        if (lang.getProbability() < minProbability && languages.size() > 1)
            return null;

        return lang.getLocale().getLanguage();
    }

}
