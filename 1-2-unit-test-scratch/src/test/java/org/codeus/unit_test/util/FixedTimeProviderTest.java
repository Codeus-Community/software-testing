package org.codeus.unit_test.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FixedTimeProviderTest {

    private LocalDateTime fixedTime;

    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    }

    @Test
    void now_ReturnsFixedTime() {
        // Arrange
        FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);

        // Act
        LocalDateTime result = timeProvider.now();

        // Assert
        assertThat(result).isEqualTo(fixedTime);
    }

    @Test
    void now_CalledMultipleTimes_ReturnsTheSameTime() {
        // Arrange
        FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);

        // Act
        LocalDateTime first = timeProvider.now();
        LocalDateTime second = timeProvider.now();
        LocalDateTime third = timeProvider.now();

        // Assert
        assertThat(first).isEqualTo(fixedTime);
        assertThat(second).isEqualTo(fixedTime);
        assertThat(third).isEqualTo(fixedTime);
    }

    @Test
    void setTime_ChangesReturnedTime() {
        // Arrange
        FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
        LocalDateTime newTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        // Act
        timeProvider.setTime(newTime);
        LocalDateTime result = timeProvider.now();

        // Assert
        assertThat(result).isEqualTo(newTime);
        assertThat(result).isNotEqualTo(fixedTime);
    }

    @Test
    void setTime_CalledMultipleTimes_ReturnsLatestTime() {
        // Arrange
        FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
        LocalDateTime time1 = LocalDateTime.of(2024, 3, 1, 12, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 6, 15, 18, 30, 0);

        // Act
        timeProvider.setTime(time1);
        LocalDateTime result1 = timeProvider.now();

        timeProvider.setTime(time2);
        LocalDateTime result2 = timeProvider.now();

        // Assert
        assertThat(result1).isEqualTo(time1);
        assertThat(result2).isEqualTo(time2);
    }

    @Test
    void constructor_WithNullTime_StillCreatesInstance() {
        // Arrange & Act
        FixedTimeProvider timeProvider = new FixedTimeProvider(null);
        LocalDateTime result = timeProvider.now();

        // Assert
        assertThat(result).isNull();
    }
}