package org.codeus.unit_test.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FixedTimeProviderTest {

    private LocalDateTime fixedTime;

    /**
     * Setup executed before each test.
     * Initializes a fixed time for testing.
     */
    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    }

    /**
     * Demonstrates: Why FixedTimeProvider is essential for deterministic tests
     * FIRST principles: Repeatable (same input = same output, always)
     *
     * Tests that FixedTimeProvider returns the exact same time every call.
     * This is critical for testing time-dependent logic:
     *
     * Problem with LocalDateTime.now():
     * - Returns different value each millisecond
     * - Tests become flaky (sometimes pass, sometimes fail)
     * - Can't verify exact timestamps in assertions
     *
     * Solution with FixedTimeProvider:
     * - Returns consistent, predictable time
     * - Tests are deterministic and repeatable
     * - Can assert exact values: account.getCreatedAt() == fixedTime
     *
     * This enables the "Timely" principle - code must be designed for testability.
     */
    @Test
    void now_ReturnsFixedTime() {
        // Arrange
        FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);

        // Act
        LocalDateTime result = timeProvider.now();

        // Assert
        assertThat(result).isEqualTo(fixedTime);
    }

    /**
     * Demonstrates: Testing stateful behavior - object maintains and changes internal state
     * FIRST principles: Independent (test doesn't depend on external time source)
     *
     * Tests that FixedTimeProvider can update its internal time.
     * This allows simulating time progression in tests:
     * - Create account at time T1
     * - Set time to T2 (1 month later)
     * - Calculate interest for period T1 to T2
     *
     * Without this, you'd have to wait real time or use unreliable system clock manipulation.
     */
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
}