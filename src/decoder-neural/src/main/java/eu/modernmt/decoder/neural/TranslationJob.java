package eu.modernmt.decoder.neural;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.io.IOException;
import java.util.UUID;

class TranslationJob {

    private final NeuralDecoder decoder;
    private final UUID user;
    private final LanguageDirection direction;
    private final Sentence text;
    private final ContextVector contextVector;

    private ScoreEntry[] suggestions = null;
    private Translation translation = null;

    public TranslationJob(NeuralDecoder decoder, UUID user, LanguageDirection direction, Sentence text, ContextVector contextVector) {
        this.decoder = decoder;
        this.user = user;
        this.direction = direction;
        this.text = text;
        this.contextVector = contextVector;
    }

    public long computeSuggestions(int limit) throws DecoderException {
        long begin = System.currentTimeMillis();

        if (text.hasWords() && contextVector != null && !contextVector.isEmpty()) {
            try {
                suggestions = decoder.getTranslationMemory().search(user, direction, text, contextVector, limit);
            } catch (IOException e) {
                throw new DecoderException("Failed to retrieve suggestions from memory", e);
            }
        }

        return System.currentTimeMillis() - begin;
    }

    public ScoreEntry[] getSuggestions() {
        return suggestions;
    }

    public long computeTranslation(PythonDecoder pythonDecoder) throws DecoderException {
        long begin = System.currentTimeMillis();

        if (text.hasWords()) {
            if (decoder.isEchoServer() || pythonDecoder == null) {
                if (suggestions != null && suggestions.length > 0) {
                    translation = Translation.fromTokens(text, suggestions[0].translation);
                } else {
                    translation = Translation.fromTokens(text, TokensOutputStream.tokens(text, false, true));
                }
            } else {
                if (suggestions != null && suggestions.length > 0) {
                    // if perfect match, force translate with suggestion instead
                    if (suggestions[0].score == 1.f) {
                        translation = pythonDecoder.translate(direction, text, suggestions[0].translation);
                    } else {
                        translation = pythonDecoder.translate(direction, text, suggestions, 0);
                    }
                } else {
                    translation = pythonDecoder.translate(direction, text, 0);
                }
            }
        } else {
            translation = Translation.emptyTranslation(text);
        }

        return System.currentTimeMillis() - begin;
    }

    public Translation getTranslation() {
        return translation;
    }
}
