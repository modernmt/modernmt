package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderException;
import eu.modernmt.lang.LanguageDirection;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Scheduler {

    interface Lock {

        void await() throws InterruptedException;

        void await(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException;

    }

    Lock schedule(LanguageDirection direction, TranslationSplit[] translationSplits) throws DecoderException;

}
