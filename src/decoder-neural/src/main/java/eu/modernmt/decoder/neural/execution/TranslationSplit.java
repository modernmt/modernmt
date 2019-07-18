package eu.modernmt.decoder.neural.execution;

import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;

public class TranslationSplit {

    public final Sentence sentence;
    public final ScoreEntry[] suggestions;

    public TranslationSplit(Sentence sentence, ScoreEntry[] suggestions) {
        this.sentence = sentence;
        this.suggestions = suggestions;
    }

}
