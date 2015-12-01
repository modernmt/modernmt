package eu.modernmt.decoder;

import eu.modernmt.model.context.TranslationContext;

import java.io.Closeable;
import java.util.Date;

/**
 * Created by davide on 30/11/15.
 */
public abstract class DecoderSession implements Closeable, AutoCloseable {

    public static final long DEFAULT_LIFETIME = 30L * 60L * 1000L;

    protected final long id;
    protected final TranslationContext translationContext;
    protected final long lifetime;
    protected Date lastActivity;

    public DecoderSession(long id, TranslationContext translationContext) {
        this(id, translationContext, DEFAULT_LIFETIME);
    }

    public DecoderSession(long id, TranslationContext translationContext, long lifetime) {
        this.id = id;
        this.translationContext = translationContext;
        this.lifetime = lifetime;
        this.lastActivity = new Date();
    }

    public long getId() {
        return id;
    }

    public long getLifetime() {
        return lifetime;
    }

    public TranslationContext getTranslationContext() {
        return translationContext;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public Date getExpiration() {
        return new Date(lifetime + lastActivity.getTime());
    }

    public boolean hasExpired() {
        return (System.currentTimeMillis() - lastActivity.getTime()) > lifetime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DecoderSession that = (DecoderSession) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
