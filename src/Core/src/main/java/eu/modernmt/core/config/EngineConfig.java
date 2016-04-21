package eu.modernmt.core.config;

import java.util.Locale;

/**
 * Created by davide on 19/04/16.
 */
public class EngineConfig {

    private String name;
    private Locale sourceLanguage;
    private Locale targetLanguage;
    private final DecoderConfig decoderConfig = new DecoderConfig();
    private final AlignerConfig alignerConfig = new AlignerConfig();

    public Locale getSourceLanguage() {
        return sourceLanguage;
    }

    public Locale getTargetLanguage() {
        return targetLanguage;
    }

    public String getName() {
        return name;
    }

    public void setSourceLanguage(Locale sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public void setTargetLanguage(Locale targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DecoderConfig getDecoderConfig() {
        return decoderConfig;
    }

    public AlignerConfig getAlignerConfig() {
        return alignerConfig;
    }
}
