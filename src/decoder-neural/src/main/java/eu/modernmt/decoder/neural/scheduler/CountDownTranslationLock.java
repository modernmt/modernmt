package eu.modernmt.decoder.neural.scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CountDownTranslationLock implements Scheduler.TranslationLock {

    private final CountDownLatch count;

    public CountDownTranslationLock(int count) {
        this.count = new CountDownLatch(count);
    }

    @Override
    public void await() throws InterruptedException {
        this.count.await();
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return this.count.await(timeout, unit);
    }

    @Override
    public void translationSplitCompleted(TranslationSplit translationSplit) {
        count.countDown();
    }

}
