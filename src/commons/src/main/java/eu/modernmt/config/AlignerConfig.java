package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class AlignerConfig {

    private final EngineConfig parent;
    protected boolean enabled = false;

    public AlignerConfig(EngineConfig parent) {
        this.parent = parent;
    }

    public EngineConfig getParentConfig() {
        return parent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "Aligner: " +
                "enabled=" + enabled;
    }

}
