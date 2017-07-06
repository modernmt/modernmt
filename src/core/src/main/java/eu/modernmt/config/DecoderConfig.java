package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public abstract class DecoderConfig {

    protected boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public abstract int getParallelismDegree();

}
