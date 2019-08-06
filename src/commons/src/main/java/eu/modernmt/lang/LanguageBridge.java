package eu.modernmt.lang;

import java.util.Objects;

public final class LanguageBridge {

    public final LanguageDirection source;
    public final LanguageDirection target;

    public LanguageBridge(LanguageDirection source, LanguageDirection target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanguageBridge that = (LanguageBridge) o;
        return source.equals(that.source) &&
                target.equals(that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public String toString() {
        return source.source + " (" + source.target + ")\u2192 " + target.target;
    }
}
