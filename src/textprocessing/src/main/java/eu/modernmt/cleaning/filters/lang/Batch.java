package eu.modernmt.cleaning.filters.lang;

import com.optimaize.langdetect.DetectedLanguage;

import java.util.List;

/**
 * Created by davide on 27/12/17.
 */
class Batch {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final int size;
    private final StringBuilder buffer = new StringBuilder();
    private int count = 0;
    private int beginIndex = -1;
    private int endIndex = -1;

    Batch() {
        this(DEFAULT_BATCH_SIZE);
    }

    Batch(int size) {
        this.size = size;
    }

    public int size() {
        return count;
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
        buffer.setLength(0);
    }

    public void add(String line, int index) {
        if (beginIndex < 0)
            beginIndex = index;
        endIndex = index;

        count++;

        buffer.append(line.toLowerCase());
        buffer.append(' ');
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public String getLanguage() {
        List<DetectedLanguage> languages = OptimaizeLanguageFilter.getLanguageDetector().getProbabilities(buffer);

        if (languages.size() < 1)
            return null;

        DetectedLanguage lang = languages.get(0);
        if (lang.getProbability() < 0.50d && languages.size() > 1)
            return null;

        return lang.getLocale().getLanguage();
    }

}