package org.codeus.unit_test.util;

import java.time.LocalDateTime;

public interface TimeProvider {
    LocalDateTime now();
}