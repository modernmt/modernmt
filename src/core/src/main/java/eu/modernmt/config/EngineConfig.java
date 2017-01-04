package eu.modernmt.config;

import java.util.Locale;

/**
 * Created by davide on 04/01/17.
 */
public class EngineConfig {

    private String name = "default";
    private Locale sourceLanguage = null;
    private Locale targetLanguage = null;
    private final DecoderConfig decoderConfig = new DecoderConfig();

    public String getName() {
        return name;
    }

    public EngineConfig setName(String name) {
        this.name = name;
        return this;
    }

    public Locale getSourceLanguage() {
        return sourceLanguage;
    }

    public EngineConfig setSourceLanguage(Locale sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
        return this;
    }

    public Locale getTargetLanguage() {
        return targetLanguage;
    }

    public EngineConfig setTargetLanguage(Locale targetLanguage) {
        this.targetLanguage = targetLanguage;
        return this;
    }

    public DecoderConfig getDecoderConfig() {
        return decoderConfig;
    }

    @Override
    public String toString() {
        return "[Engine]\n" +
                "  name = " + name + "\n" +
                "  source-language = " + sourceLanguage.toLanguageTag() + "\n" +
                "  target-language = " + targetLanguage.toLanguageTag() + "\n" +
                "  " + decoderConfig.toString().replace("\n", "\n  ");
    }
}
