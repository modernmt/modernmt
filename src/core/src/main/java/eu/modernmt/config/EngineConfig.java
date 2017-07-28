package eu.modernmt.config;

import eu.modernmt.model.LanguagePair;

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
    private Set<LanguagePair> languagePairs = null;
    private Type type = Type.PHRASE_BASED;
    private DecoderConfig decoderConfig = new PhraseBasedDecoderConfig();

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

    public Set<LanguagePair> getLanguagePairs() {
        return languagePairs;
    }

    public void setLanguagePairs(Set<LanguagePair> languagePairs) {
        this.languagePairs = new HashSet<>(languagePairs);
    }

    public void addLanguagePair(LanguagePair pair) {
        if (this.languagePairs == null)
            this.languagePairs = new HashSet<>();
        this.languagePairs.add(pair);
    }

    public DecoderConfig getDecoderConfig() {
        return decoderConfig;
    }

    @Override
    public String toString() {
        return "[Engine]\n" +
                "  name = " + name + "\n" +
                "  type = " + type + "\n" +
                "  languages = " + languagePairs + "\n" +
                "  " + decoderConfig.toString().replace("\n", "\n  ");
    }
}
