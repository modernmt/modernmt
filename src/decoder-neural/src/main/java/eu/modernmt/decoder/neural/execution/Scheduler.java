package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.util.concurrent.Future;

public interface Scheduler {

    Future<Translation> schedule(LanguageDirection direction, Sentence sentence, TranslationSplit[] translationSplits) throws DecoderException;

}
