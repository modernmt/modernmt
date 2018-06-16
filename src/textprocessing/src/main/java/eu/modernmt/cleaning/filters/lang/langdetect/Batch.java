package eu.modernmt.cleaning.filters.lang.langdetect;

import com.optimaize.langdetect.DetectedLanguage;

import java.util.List;

/**
 * Created by davide on 27/12/17.
 */
class Batch {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final int size;
    private final StringBuilder sourceBuffer = new StringBuilder();
    private final StringBuilder targetBuffer = new StringBuilder();
    private int count = 0;
    private int beginIndex = -1;
    private int endIndex = -1;

    Batch() {
        this(DEFAULT_BATCH_SIZE);
    }

    Batch(int size) {
        this.size = size;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count >= size;
    }

    public void clear() {
        count = 0;
        beginIndex = -1;
        endIndex = -1;
        sourceBuffer.setLength(0);
        targetBuffer.setLength(0);
    }

    public void add(String source, String target, int index) {
        if (beginIndex < 0)
            beginIndex = index;
        endIndex = index;

        count++;

        sourceBuffer.append(source.toLowerCase());
        sourceBuffer.append(' ');
        targetBuffer.append(target.toLowerCase());
        targetBuffer.append(' ');
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getSourceLanguage() {
        List<DetectedLanguage> languages = OptimaizeLanguageFilter.getLanguageDetector().getProbabilities(sourceBuffer);
        return detectLanguage(languages);
    }

    public String getTargetLanguage() {
        List<DetectedLanguage> languages = OptimaizeLanguageFilter.getLanguageDetector().getProbabilities(targetBuffer);
        return detectLanguage(languages);
    }

    private static String detectLanguage(List<DetectedLanguage> languages) {
        if (languages.size() < 1)
            return null;

        DetectedLanguage lang = languages.get(0);
        if (lang.getProbability() < 0.50d && languages.size() > 1)
            return null;

        return lang.getLocale().getLanguage();
    }

}
