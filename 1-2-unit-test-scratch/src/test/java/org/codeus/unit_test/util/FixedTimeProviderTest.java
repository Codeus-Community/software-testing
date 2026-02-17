package org.codeus.unit_test.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class MainPart{
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

    /**
     * Optional test cases for FixedTimeProvider - additional practice scenarios.
     */
    @Nested
    class OptionalPart {

        /**
         * Demonstrates: Multiple setTime calls in sequence
         * FIRST principles: Fast (pure state manipulation), Repeatable
         * <p>
         * Tests that FixedTimeProvider correctly updates time on each setTime call.
         * Verifies stateful behavior - object maintains current time value.
         * This simulates time progression in tests: T1 -> T2 -> T3
         */
        @Test
        void setTime_MultipleTimes_UpdatesTimeCorrectly() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime time2 = LocalDateTime.of(2024, 6, 15, 14, 30);
            LocalDateTime time3 = LocalDateTime.of(2024, 12, 31, 23, 59);

            // Act & Assert
            timeProvider.setTime(time1);
            assertThat(timeProvider.now()).isEqualTo(time1);

            timeProvider.setTime(time2);
            assertThat(timeProvider.now()).isEqualTo(time2);

            timeProvider.setTime(time3);
            assertThat(timeProvider.now()).isEqualTo(time3);
        }

        /**
         * Demonstrates: Consistency verification - now() returns same value
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that multiple calls to now() return exactly the same time.
         * This is the core property that makes FixedTimeProvider useful for testing.
         * Unlike LocalDateTime.now() which changes every millisecond.
         */
        @Test
        void now_CalledMultipleTimes_ReturnsSameValue() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);

            // Act
            LocalDateTime firstCall = timeProvider.now();
            LocalDateTime secondCall = timeProvider.now();
            LocalDateTime thirdCall = timeProvider.now();

            // Assert
            assertThat(firstCall).isEqualTo(secondCall);
            assertThat(secondCall).isEqualTo(thirdCall);
            assertThat(firstCall).isEqualTo(fixedTime);
        }

        /**
         * Demonstrates: Testing time advancement simulation
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests simulating forward time progression using setTime.
         * Useful for testing scenarios like:
         * - Interest accrual over time
         * - Expiration dates
         * - Time-based business rules
         * Advances time by 30 days, then by 1 year.
         */
        @Test
        void setTime_AdvancingTime_SimulatesTimeProgression() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime after30Days = fixedTime.plusDays(30);
            LocalDateTime after1Year = fixedTime.plusYears(1);

            // Act & Assert
            assertThat(timeProvider.now()).isEqualTo(fixedTime);

            timeProvider.setTime(after30Days);
            assertThat(timeProvider.now()).isEqualTo(after30Days);
            assertThat(timeProvider.now()).isAfter(fixedTime);

            timeProvider.setTime(after1Year);
            assertThat(timeProvider.now()).isEqualTo(after1Year);
            assertThat(timeProvider.now()).isAfter(after30Days);
        }

        /**
         * Demonstrates: Testing backward time movement (edge case)
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that time can be moved backward.
         * While unusual, this is valid for testing scenarios like:
         * - Daylight saving time changes
         * - Time zone adjustments
         * - Testing edge cases where timestamps might be corrected
         */
        @Test
        void setTime_BackwardTime_UpdatesTimeCorrectly() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime pastTime = fixedTime.minusDays(10);

            // Act
            timeProvider.setTime(pastTime);

            // Assert
            assertThat(timeProvider.now()).isEqualTo(pastTime);
            assertThat(timeProvider.now()).isBefore(fixedTime);
        }

        /**
         * Demonstrates: Edge case - setting time to epoch (very old date)
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that FixedTimeProvider handles very old dates correctly.
         * Unix epoch: 1970-01-01 00:00:00
         * This ensures no assumptions about minimum date values.
         */
        @Test
        void setTime_WithEpochTime_WorksCorrectly() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

            // Act
            timeProvider.setTime(epoch);

            // Assert
            assertThat(timeProvider.now()).isEqualTo(epoch);
        }

        /**
         * Demonstrates: Edge case - setting time to far future
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that FixedTimeProvider handles far future dates correctly.
         * Year 2999 - tests upper boundary of date handling.
         * Useful for testing long-term projections or calculations.
         */
        @Test
        void setTime_WithFarFutureTime_WorksCorrectly() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime farFuture = LocalDateTime.of(2999, 12, 31, 23, 59, 59);

            // Act
            timeProvider.setTime(farFuture);

            // Assert
            assertThat(timeProvider.now()).isEqualTo(farFuture);
        }

        /**
         * Demonstrates: Construction with different initial times
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that FixedTimeProvider can be initialized with any time.
         * Verifies constructor properly sets initial state.
         * Different instances can have different times.
         */
        @Test
        void constructor_WithDifferentTimes_CreatesIndependentProviders() {
            // Arrange
            LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime time2 = LocalDateTime.of(2024, 12, 31, 23, 59);

            // Act
            FixedTimeProvider provider1 = new FixedTimeProvider(time1);
            FixedTimeProvider provider2 = new FixedTimeProvider(time2);

            // Assert
            assertThat(provider1.now()).isEqualTo(time1);
            assertThat(provider2.now()).isEqualTo(time2);
            assertThat(provider1.now()).isNotEqualTo(provider2.now());
        }

        /**
         * Demonstrates: Time precision with nanoseconds
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that FixedTimeProvider maintains full LocalDateTime precision.
         * LocalDateTime supports nanosecond precision.
         * Important for testing financial timestamps where precision matters.
         */
        @Test
        void now_MaintainsNanosecondPrecision() {
            // Arrange
            LocalDateTime preciseTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123456789);
            FixedTimeProvider timeProvider = new FixedTimeProvider(preciseTime);

            // Act
            LocalDateTime result = timeProvider.now();

            // Assert
            assertThat(result).isEqualTo(preciseTime);
            assertThat(result.getNano()).isEqualTo(123456789);
        }

        /**
         * Demonstrates: Midnight boundary test
         * FIRST principles: Fast, Independent
         * <p>
         * Tests setting time to exact midnight (00:00:00).
         * Midnight is a common boundary in date calculations.
         * Important for daily interest calculations, daily limits, etc.
         */
        @Test
        void setTime_ToMidnight_WorksCorrectly() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime midnight = LocalDateTime.of(2024, 1, 15, 0, 0, 0);

            // Act
            timeProvider.setTime(midnight);

            // Assert
            assertThat(timeProvider.now()).isEqualTo(midnight);
            assertThat(timeProvider.now().getHour()).isEqualTo(0);
            assertThat(timeProvider.now().getMinute()).isEqualTo(0);
            assertThat(timeProvider.now().getSecond()).isEqualTo(0);
        }

        /**
         * Demonstrates: End of day boundary test
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests setting time to last moment of day (23:59:59).
         * Another important boundary for daily calculations.
         * Complements midnight test - tests both day boundaries.
         */
        @Test
        void setTime_ToEndOfDay_WorksCorrectly() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime endOfDay = LocalDateTime.of(2024, 1, 15, 23, 59, 59);

            // Act
            timeProvider.setTime(endOfDay);

            // Assert
            assertThat(timeProvider.now()).isEqualTo(endOfDay);
            assertThat(timeProvider.now().getHour()).isEqualTo(23);
            assertThat(timeProvider.now().getMinute()).isEqualTo(59);
            assertThat(timeProvider.now().getSecond()).isEqualTo(59);
        }

        /**
         * Demonstrates: Year boundary test (New Year)
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests setting time to year transition moment.
         * Tests edge case at year boundary: 2024 -> 2025
         * Important for annual calculations, year-end processing.
         */
        @Test
        void setTime_ToYearBoundary_WorksCorrectly() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime newYear = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

            // Act
            timeProvider.setTime(newYear);

            // Assert
            assertThat(timeProvider.now()).isEqualTo(newYear);
            assertThat(timeProvider.now().getYear()).isEqualTo(2025);
            assertThat(timeProvider.now().getMonth().getValue()).isEqualTo(1);
            assertThat(timeProvider.now().getDayOfMonth()).isEqualTo(1);
        }

        /**
         * Demonstrates: Leap year date handling
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that FixedTimeProvider handles leap year dates correctly.
         * February 29, 2024 is valid (2024 is leap year).
         * Important edge case for date-based calculations.
         */
        @Test
        void setTime_ToLeapYearDate_WorksCorrectly() {
            // Arrange
            FixedTimeProvider timeProvider = new FixedTimeProvider(fixedTime);
            LocalDateTime leapDay = LocalDateTime.of(2024, 2, 29, 12, 0, 0);

            // Act
            timeProvider.setTime(leapDay);

            // Assert
            assertThat(timeProvider.now()).isEqualTo(leapDay);
            assertThat(timeProvider.now().getMonth().getValue()).isEqualTo(2);
            assertThat(timeProvider.now().getDayOfMonth()).isEqualTo(29);
        }
    }
}