package eu.modernmt.decoder.moses;

import java.io.Serializable;

/**
 * Created by davide on 30/11/15.
 */
public class MosesFeature implements Serializable {

    public static final float UNTUNEABLE_COMPONENT = Float.MAX_VALUE;

    private String name;
    private boolean tunable;
    private boolean stateless;
    private transient long ptr;

    public MosesFeature(String name, boolean tunable, boolean stateless, long ptr) {
        this.name = name;
        this.tunable = tunable;
        this.stateless = stateless;
        this.ptr = ptr;
    }

    public String getName() {
        return name;
    }

    public boolean isTunable() {
        return tunable;
    }

    public boolean isStateless() {
        return stateless;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MosesFeature feature = (MosesFeature) o;

        return name.equals(feature.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
