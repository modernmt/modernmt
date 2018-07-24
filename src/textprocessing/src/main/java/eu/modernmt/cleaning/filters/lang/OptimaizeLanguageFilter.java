package eu.modernmt.cleaning.filters.lang;

import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import eu.modernmt.cleaning.Filter;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 27/12/17.
 */
public class OptimaizeLanguageFilter implements Filter {

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

    private final HashMap<LanguagePair, Blacklist> blacklists = new HashMap<>();

    private static boolean isSupported(LanguagePair language) {
        return SUPPORTED_LANGUAGES.contains(language.source.getLanguage()) &&
                SUPPORTED_LANGUAGES.contains(language.target.getLanguage());
    }

    @Override
    public Initializer getInitializer() {
        return new Initializer() {

            private HashMap<LanguagePair, Batch> batches = new HashMap<>();

            @Override
            public void onBegin() {
                blacklists.clear();
            }

            @Override
            public void onPair(MultilingualCorpus corpus, MultilingualCorpus.StringPair pair, int index) {
                if (isSupported(pair.language)) {
                    Batch batch = batches.computeIfAbsent(pair.language, (key) -> new Batch());
                    batch.add(pair.source, pair.target, index);

                    if (batch.isFull())
                        analyze(pair.language, batch);
                }
            }

            private void analyze(LanguagePair direction, Batch batch) {
                String sourceLang = batch.getSourceLanguage();
                String targetLang = batch.getTargetLanguage();

                if (!direction.source.getLanguage().equalsIgnoreCase(sourceLang) ||
                        !direction.target.getLanguage().equalsIgnoreCase(targetLang)) {
                    int beginIndex = batch.getBeginIndex();
                    int endIndex = batch.getEndIndex();

                    Blacklist blacklist = blacklists.computeIfAbsent(direction, (key) -> new Blacklist());
                    blacklist.add(beginIndex, endIndex);
                }

                batch.clear();
            }

            @Override
            public void onEnd() {
                for (Map.Entry<LanguagePair, Batch> entry : batches.entrySet()) {
                    if (!entry.getValue().isEmpty())
                        analyze(entry.getKey(), entry.getValue());
                }

                blacklists.entrySet().removeIf(entry -> entry.getValue().size() < MIN_SIZE);
            }
        };
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) {
        Blacklist blacklist = blacklists.get(pair.language);
        return blacklist == null || !blacklist.contains(index);
    }

    @Override
    public void clear() {
        blacklists.clear();
    }

}
