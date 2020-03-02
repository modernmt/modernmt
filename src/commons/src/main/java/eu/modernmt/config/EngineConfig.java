package eu.modernmt.config;

import eu.modernmt.lang.LanguageIndex;

/**
 * Created by davide on 04/01/17.
 */
public class EngineConfig {

    private final NodeConfig parent;
    private String name = "default";
    private LanguageIndex languageIndex = null;
    private DecoderConfig decoderConfig = new DecoderConfig(this);
    private AlignerConfig alignerConfig = new AlignerConfig(this);
    private AnalyzerConfig analyzerConfig = new AnalyzerConfig(this);

    public EngineConfig(NodeConfig parent) {
        this.parent = parent;
    }

    public NodeConfig getParentConfig() {
        return parent;
    }

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

    public AnalyzerConfig getAnalyzerConfig() {
        return analyzerConfig;
    }

    @Override
    public String toString() {
        return "Engine: " +
                "name='" + name + '\'' +
                ", languages=" + languageIndex.size() +
                "\n  " + decoderConfig.toString().replace("\n", "\n  ") +
                "\n  " + alignerConfig.toString().replace("\n", "\n  ") +
                "\n  " + analyzerConfig.toString().replace("\n", "\n  ");
    }
}
