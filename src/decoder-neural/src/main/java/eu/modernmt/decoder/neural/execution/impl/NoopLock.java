package eu.modernmt.decoder.neural.execution.impl;

import eu.modernmt.decoder.neural.execution.Scheduler;

import java.util.concurrent.TimeUnit;

public class NoopLock implements Scheduler.Lock {

    public static final NoopLock INSTANCE = new NoopLock();

    private NoopLock() {
    }

    @Override
    public void await() {
    }

    @Override
    public void await(long timeout, TimeUnit unit) {
    }

}
