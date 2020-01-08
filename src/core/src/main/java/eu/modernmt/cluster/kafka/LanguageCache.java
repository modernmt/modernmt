package eu.modernmt.cluster.kafka;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;

import java.util.concurrent.ConcurrentHashMap;

class LanguageCache {

    private static final ConcurrentHashMap<LanguageDirection, LanguageDirection> cache = new ConcurrentHashMap<>();

    public static LanguageDirection defaultMapping(LanguageDirection direction) {
        return cache.computeIfAbsent(direction, d -> {
            Language source = direction.source.isLanguageOnly() ? null : new Language(direction.source.getLanguage());
            Language target = direction.target.isLanguageOnly() ? null : new Language(direction.target.getLanguage());

            if (source == null && target == null)
                return d;
            else if (source == null)
                source = direction.source;
            else if (target == null)
                target = direction.target;

            return new LanguageDirection(source, target);
        });
    }
}
