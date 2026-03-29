package org.codeus.unit_test.util;

import java.time.LocalDateTime;

public class FixedTimeProvider implements TimeProvider {
    private LocalDateTime fixedTime;

    public FixedTimeProvider(LocalDateTime fixedTime) {
        this.fixedTime = fixedTime;
    }

    @Override
    public LocalDateTime now() {
        return fixedTime;
    }

    public void setTime(LocalDateTime time) {
        this.fixedTime = time;
    }
}