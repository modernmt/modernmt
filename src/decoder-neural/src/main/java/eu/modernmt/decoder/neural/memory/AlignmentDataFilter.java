package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;

import java.util.HashMap;
import java.util.Map;

public class AlignmentDataFilter implements TranslationMemory.DataFilter {

    private final HashMap<LanguagePair, Float> thresholds;

    public AlignmentDataFilter(Map<LanguagePair, Float> thresholds) {
        this.thresholds = new HashMap<>(thresholds.size() * 2);

        for (Map.Entry<LanguagePair, Float> entry : thresholds.entrySet()) {
            this.thresholds.put(entry.getKey(), entry.getValue());
            this.thresholds.put(entry.getKey().reversed(), entry.getValue());
        }
    }

    @Override
    public boolean accept(TranslationUnit unit) {
        if (unit.alignment == null)
            return true;

        Float threshold = thresholds.get(unit.direction);
        return threshold == null || unit.alignment.getScore() >= threshold;
    }

}
