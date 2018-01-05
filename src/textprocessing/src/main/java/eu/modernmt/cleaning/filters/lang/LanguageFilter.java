package eu.modernmt.cleaning.filters.lang;

import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import eu.modernmt.cleaning.MultilingualCorpusFilter;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by davide on 27/12/17.
 */
public class LanguageFilter implements MultilingualCorpusFilter {

    private static final int MIN_SIZE = 50;
    private static LanguageDetector detectorInstance = null;

    public static LanguageDetector getLanguageDetector() {
        if (detectorInstance == null) {
            synchronized (LanguageFilter.class) {
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

    @Override
    public FilterInitializer getInitializer() {
        return new FilterInitializer() {

            private HashMap<LanguagePair, Batch> batches = new HashMap<>();

            @Override
            public void onBegin() {
                blacklists.clear();
            }

            @Override
            public void onPair(MultilingualCorpus corpus, MultilingualCorpus.StringPair pair, int index) {
                Batch batch = batches.computeIfAbsent(pair.language, (key) -> new Batch());
                batch.add(pair.source, pair.target, index);

                if (batch.isFull())
                    analyze(pair.language, batch);
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
