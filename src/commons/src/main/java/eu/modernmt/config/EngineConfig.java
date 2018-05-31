package eu.modernmt.config;

import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by davide on 04/01/17.
 */
public class EngineConfig {

    public enum Type {
        PHRASE_BASED, NEURAL
    }

    private String name = "default";
    private LanguageIndex languageIndex = null;
    private Type type = Type.PHRASE_BASED;
    private DecoderConfig decoderConfig = new PhraseBasedDecoderConfig();
    private AlignerConfig alignerConfig = new AlignerConfig();

    public String getName() {
        return name;
    }

    public EngineConfig setName(String name) {
        this.name = name;
        return this;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        if (type == this.type)
            return;

        this.type = type;
        this.decoderConfig = type == Type.PHRASE_BASED ? new PhraseBasedDecoderConfig() : new NeuralDecoderConfig();
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
                "  type = " + type + "\n" +
                "  languages = " + languageIndex + "\n" +
                "  " + decoderConfig.toString().replace("\n", "\n  ");
    }
}
