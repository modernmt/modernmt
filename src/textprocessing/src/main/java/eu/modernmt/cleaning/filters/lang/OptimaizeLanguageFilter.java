package eu.modernmt.cleaning.filters.lang;

import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import eu.modernmt.cleaning.CorpusFilter;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.Language;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by davide on 27/12/17.
 */
public class OptimaizeLanguageFilter implements CorpusFilter {

    private static final HashSet<String> SUPPORTED_LANGUAGES = new HashSet<>(
            Arrays.asList("af", "an", "ar", "ast", "be", "br", "ca", "bg", "bn", "cs", "cy", "da", "de", "el", "en",
                    "es", "et", "eu", "fa", "fi", "fr", "ga", "gl", "gu", "he", "hi", "hr", "ht", "hu", "id", "is",
                    "it", "ja", "km", "kn", "ko", "lt", "lv", "mk", "ml", "mr", "ms", "mt", "ne", "nl", "no", "oc",
                    "pa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl",
                    "tr", "uk", "ur", "vi", "wa", "yi", "zh"));

    private static final int MIN_SIZE = 50;
    private static LanguageDetector detectorInstance = null;

    public static LanguageDetector getLanguageDetector() {
        if (detectorInstance == null) {
            synchronized (OptimaizeLanguageFilter.class) {
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

    private Blacklist blacklist = null;

    private static boolean isSupported(Language language) {
        return SUPPORTED_LANGUAGES.contains(language.getLanguage());
    }

    @Override
    public Initializer getInitializer(Language language) {
        return new Initializer() {

            private final Batch batch = new Batch();

            @Override
            public void onBegin() {
                blacklist = null;
            }

            @Override
            public void onLine(String line, int index) {
                if (isSupported(language)) {
                    batch.add(line, index);

                    if (batch.isFull())
                        analyze(batch);
                }
            }

            private void analyze(Batch batch) {
                String lang = batch.getLanguage();

                if (!language.getLanguage().equalsIgnoreCase(lang)) {
                    int beginIndex = batch.getBeginIndex();
                    int endIndex = batch.getEndIndex();

                    if (blacklist == null)
                        blacklist = new Blacklist();

                    blacklist.add(beginIndex, endIndex);
                }

                batch.clear();
            }

            @Override
            public void onEnd() {
                if (!batch.isEmpty())
                    analyze(batch);

                if (blacklist.size() < MIN_SIZE)
                    blacklist = null;
            }
        };
    }

    @Override
    public boolean accept(String line, int index) {
        return blacklist == null || !blacklist.contains(index);
    }

    @Override
    public void clear() {
        this.blacklist = null;
    }

}
