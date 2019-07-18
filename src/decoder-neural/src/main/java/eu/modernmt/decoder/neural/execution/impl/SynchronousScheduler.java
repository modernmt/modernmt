package eu.modernmt.decoder.neural.execution.impl;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.execution.DecoderQueue;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.decoder.neural.execution.Scheduler;
import eu.modernmt.decoder.neural.execution.TranslationSplit;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Translation;

import java.util.concurrent.Future;

public class SynchronousScheduler implements Scheduler {

    private final DecoderQueue queue;

    public SynchronousScheduler(DecoderQueue queue) {
        this.queue = queue;
    }

    @Override
    public Future<Void> schedule(LanguageDirection direction, TranslationSplit[] splits) throws DecoderException {
        PythonDecoder decoder = null;

        try {
            decoder = queue == null ? null : queue.take(direction);

            for (TranslationSplit split : splits) {
                Translation translation = translate(decoder, direction, split);
                split.setTranslation(translation);
            }

            return NoopFuture.INSTANCE;
        } finally {
            if (decoder != null)
                queue.release(decoder);
        }
    }

    private Translation translate(PythonDecoder decoder, LanguageDirection direction, TranslationSplit split) throws DecoderException {
        if (split.sentence.hasWords()) {
            if (decoder == null) {
                if (split.suggestions != null && split.suggestions.length > 0) {
                    return Translation.fromTokens(split.sentence, split.suggestions[0].translation);
                } else {
                    return Translation.fromTokens(split.sentence, TokensOutputStream.tokens(split.sentence, false, true));
                }
            } else {
                if (split.suggestions != null && split.suggestions.length > 0) {
                    // if perfect match, force translate with suggestion instead
                    if (split.suggestions[0].score == 1.f) {
                        return decoder.align(direction, split.sentence, split.suggestions[0].translation);
                    } else {
                        return decoder.translate(direction, split.sentence, split.suggestions, 0);
                    }
                } else {
                    return decoder.translate(direction, split.sentence, 0);
                }
            }
        } else {
            return Translation.emptyTranslation(split.sentence);
        }
    }
}
