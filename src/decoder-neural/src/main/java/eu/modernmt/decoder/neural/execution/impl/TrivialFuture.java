package eu.modernmt.decoder.neural.execution.impl;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TrivialFuture<V> implements Future<V> {

    private final V value;

    public TrivialFuture(V value) {
        this.value = value;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public V get() {
        return value;
    }

    @Override
    public V get(long timeout, @NotNull TimeUnit unit) {
        throw new UnsupportedOperationException();
    }
}
