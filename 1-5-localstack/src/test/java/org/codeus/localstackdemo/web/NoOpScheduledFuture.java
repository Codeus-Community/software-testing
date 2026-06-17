package org.codeus.localstackdemo.web;

import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class NoOpScheduledFuture implements ScheduledFuture<Object> {

    static final NoOpScheduledFuture INSTANCE = new NoOpScheduledFuture();

    private NoOpScheduledFuture() {
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
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
        return false;
    }

    @Override
    public Object get() {
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) {
        return null;
    }
}
