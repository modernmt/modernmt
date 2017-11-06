package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class AlignerConfig {

    protected boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "[AlignerConfig]\n" +
                "  enabled = " + this.enabled;
    }

}
