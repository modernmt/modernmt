package eu.modernmt.config;

import eu.modernmt.lang.LanguageIndex;

/**
 * Created by davide on 04/01/17.
 */
public class EngineConfig {

    private String name = "default";
    private LanguageIndex languageIndex = null;
    private DecoderConfig decoderConfig = new DecoderConfig();
    private AlignerConfig alignerConfig = new AlignerConfig();

    public String getName() {
        return name;
    }

    public EngineConfig setName(String name) {
        this.name = name;
        return this;
    }

    public LanguageIndex getLanguageIndex() {
        return languageIndex;
    }

    public void setLanguageIndex(LanguageIndex languageIndex) {
        this.languageIndex = languageIndex;
    }

    public DecoderConfig getDecoderConfig() {
        return decoderConfig;
    }

    public AlignerConfig getAlignerConfig() {
        return alignerConfig;
    }

    @Override
    public String toString() {
        return "[Engine]\n" +
                "  name = " + name + "\n" +
                "  languages = " + languageIndex + "\n" +
                "  " + decoderConfig.toString().replace("\n", "\n  ");
    }
}
