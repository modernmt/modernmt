package eu.modernmt.decoder.neural.execution;

import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.lang.LanguageDirection;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

public interface Scheduler extends Closeable {

    interface TranslationLock {

        TranslationLock NOOP = new TranslationLock() {
            @Override
            public void await() {
            }

            @Override
            public boolean await(long timeout, TimeUnit unit) {
                return true;
            }

            @Override
            public void translationSplitCompleted(TranslationSplit split) {
            }
        };

        void await() throws InterruptedException;

        boolean await(long timeout, TimeUnit unit) throws InterruptedException;

        void translationSplitCompleted(TranslationSplit split);

    }

    TranslationLock schedule(LanguageDirection direction, TranslationSplit[] translationSplits) throws DecoderUnavailableException;

}
