package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.lang.LanguageDirection;

import java.util.concurrent.Future;

public interface Scheduler {

    Future<Void> schedule(LanguageDirection direction, TranslationSplit[] translationSplits) throws DecoderException;

}
