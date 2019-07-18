package eu.modernmt.decoder.neural.execution;

import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

public class TranslationSplit {

    public final Sentence sentence;
    public final ScoreEntry[] suggestions;
    private Translation translation;

    public TranslationSplit(Sentence sentence, ScoreEntry[] suggestions) {
        this.sentence = sentence;
        this.suggestions = suggestions;
    }

    public Translation getTranslation() {
        return translation;
    }

    public void setTranslation(Translation translation) {
        this.translation = translation;
    }
}
