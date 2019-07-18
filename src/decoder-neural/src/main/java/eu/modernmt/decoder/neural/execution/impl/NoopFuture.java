package eu.modernmt.decoder.neural.execution.impl;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NoopFuture implements Future<Void> {

    public static final NoopFuture INSTANCE = new NoopFuture();

    private NoopFuture() {
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public Void get() {
        return null;
    }

    @Override
    public Void get(long timeout, @NotNull TimeUnit unit) {
        return null;
    }
}
