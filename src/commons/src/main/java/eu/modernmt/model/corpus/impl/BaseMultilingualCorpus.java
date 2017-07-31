package eu.modernmt.model.corpus.impl;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpora;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by davide on 31/07/17.
 */
public abstract class BaseMultilingualCorpus implements MultilingualCorpus {

    private Map<LanguagePair, Integer> _counts = null;

    private Map<LanguagePair, Integer> getCounts() {
        if (_counts == null) {
            synchronized (this) {
                if (_counts == null)
                    try {
                        _counts = Corpora.countLines(this);
                    } catch (IOException e) {
                        _counts = new HashMap<>();
                    }
            }
        }

        return _counts;
    }

    @Override
    public final Set<LanguagePair> getLanguages() {
        return getCounts().keySet();
    }

    @Override
    public final int getLineCount(LanguagePair language) {
        Integer count = getCounts().get(language);
        return count == null ? 0 : count;
    }

}
