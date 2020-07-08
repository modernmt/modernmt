package eu.modernmt.model.corpus;

import eu.modernmt.lang.LanguageDirection;

import java.util.Date;
import java.util.Objects;

public class TranslationUnit {

    public String tuid;
    public LanguageDirection language;
    public String source;
    public String target;
    public Date timestamp;

    public TranslationUnit(String tuid, LanguageDirection language, String source, String target) {
        this(tuid, language, source, target, null);
    }

    public TranslationUnit(String tuid, LanguageDirection language, String source, String target, Date timestamp) {
        this.tuid = tuid;
        this.language = language;
        this.source = source;
        this.target = target;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return '[' + language.toString() + "]<" + source + " ||| " + target + '>';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TranslationUnit that = (TranslationUnit) o;
        return Objects.equals(tuid, that.tuid) &&
                language.equals(that.language) &&
                source.equals(that.source) &&
                target.equals(that.target) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tuid, language, source, target, timestamp);
    }
}
