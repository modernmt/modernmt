package eu.modernmt.model.corpus;

import eu.modernmt.lang.LanguageDirection;

import java.util.Date;

public class TranslationUnit {

    public LanguageDirection language;
    public String source;
    public String target;
    public Date timestamp;

    public TranslationUnit(LanguageDirection language, String source, String target) {
        this(language, source, target, null);
    }

    public TranslationUnit(LanguageDirection language, String source, String target, Date timestamp) {
        this.language = language;
        this.source = source;
        this.target = target;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TranslationUnit that = (TranslationUnit) o;

        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        if (!language.equals(that.language)) return false;
        if (!source.equals(that.source)) return false;
        return target.equals(that.target);
    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + language.hashCode();
        result = 31 * result + source.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return '[' + language.toString() + "]<" + source + " ||| " + target + '>';
    }

}
