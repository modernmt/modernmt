package eu.modernmt.lang;

import java.io.Serializable;

/**
 * Created by davide on 27/07/17.
 */
public final class LanguageDirection implements Serializable {

    public final Language source;
    public final Language target;
    private transient LanguageDirection reversed = null;

    public LanguageDirection(Language source, Language target) {
        this.source = source;
        this.target = target;
    }

    public LanguageDirection reversed() {
        if (reversed == null) {
            LanguageDirection reversed = new LanguageDirection(target, source);
            reversed.reversed = this;

            this.reversed = reversed;
        }

        return reversed;
    }

    public boolean equalsIgnoreDirection(LanguageDirection o) {
        if (o == null) return false;
        return (source.equals(o.source) && target.equals(o.target)) || (source.equals(o.target) && target.equals(o.source));
    }

    public boolean equalsIgnoreCountry(LanguageDirection o) {
        if (o == null) return false;
        return source.getLanguage().equals(o.source.getLanguage()) && target.getLanguage().equals(o.target.getLanguage());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LanguageDirection that = (LanguageDirection) o;

        if (!source.equals(that.source)) return false;
        return target.equals(that.target);
    }

    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return source + " \u2192 " + target;
    }

}
