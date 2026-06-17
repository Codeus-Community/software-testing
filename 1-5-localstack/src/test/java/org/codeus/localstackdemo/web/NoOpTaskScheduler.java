package org.codeus.localstackdemo.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

final class NoOpTaskScheduler implements TaskScheduler {

    @Override
    public Clock getClock() {
        return Clock.systemUTC();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        return NoOpScheduledFuture.INSTANCE;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
        return NoOpScheduledFuture.INSTANCE;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
        return NoOpScheduledFuture.INSTANCE;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
        return NoOpScheduledFuture.INSTANCE;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
        return NoOpScheduledFuture.INSTANCE;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
        return NoOpScheduledFuture.INSTANCE;
    }
}
