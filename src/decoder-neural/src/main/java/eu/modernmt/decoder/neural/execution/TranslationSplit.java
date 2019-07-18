package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

public class TranslationSplit {

    public final Sentence sentence;
    public final ScoreEntry[] suggestions;
    private Translation translation;
    private DecoderException exception;

    public TranslationSplit(Sentence sentence, ScoreEntry[] suggestions) {
        this.sentence = sentence;
        this.suggestions = suggestions;
    }

    public Translation getTranslation() throws DecoderException {
        if (exception == null)
            return translation;
        throw exception;
    }

    public void setTranslation(Translation translation) {
        this.translation = translation;
    }

    public void setException(DecoderException exception) {
        this.exception = exception;
    }
}
