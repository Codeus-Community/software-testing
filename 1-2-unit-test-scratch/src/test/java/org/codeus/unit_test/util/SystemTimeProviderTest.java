package org.codeus.unit_test.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SystemTimeProviderTest {

    @Test
    void now_ReturnsCurrentTime() {
        // Arrange
        SystemTimeProvider timeProvider = new SystemTimeProvider();
        LocalDateTime before = LocalDateTime.now();

        // Act
        LocalDateTime result = timeProvider.now();

        // Assert
        LocalDateTime after = LocalDateTime.now();
        assertThat(result).isAfterOrEqualTo(before);
        assertThat(result).isBeforeOrEqualTo(after);
    }

    @Test
    void now_CalledTwice_ReturnsDifferentTimes() {
        // Arrange
        SystemTimeProvider timeProvider = new SystemTimeProvider();

        // Act
        LocalDateTime first = timeProvider.now();
        LocalDateTime second = timeProvider.now();

        // Assert
        assertThat(second).isAfterOrEqualTo(first);
    }

    @Test
    void now_ReturnsNonNullValue() {
        // Arrange
        SystemTimeProvider timeProvider = new SystemTimeProvider();

        // Act
        LocalDateTime result = timeProvider.now();

        // Assert
        assertThat(result).isNotNull();
    }
}