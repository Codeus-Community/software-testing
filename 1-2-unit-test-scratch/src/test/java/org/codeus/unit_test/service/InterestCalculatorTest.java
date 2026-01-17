package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class InterestCalculatorTest {

    private InterestCalculator calculator;
    private Account savingsAccount;
    private LocalDateTime startDate;

    /**
     * Setup executed before each test.
     * Initializes calculator and test data for consistent test conditions.
     */
    @BeforeEach
    void setUp() {
        calculator = new InterestCalculator();
        startDate = LocalDateTime.of(2024, 1, 1, 0, 0);

        savingsAccount = Account.builder()
                .id("acc-001")
                .type(AccountType.SAVINGS)
                .currency(Currency.USD)
                .balance(new BigDecimal("1000"))
                .build();
    }

    @Nested
    class MainPart {
        /**
         * Demonstrates: Testing pure functions without any mocks or external dependencies
         * FIRST principles: Fast (pure calculation), Independent (no external state),
         * Repeatable (deterministic mathematical operation)
         * <p>
         * Tests simple interest calculation for a 30-day period.
         * Shows how to test mathematical calculations with BigDecimal precision.
         * Pure function testing - no mocks needed, just input and expected output.
         */
        @Test
        void calculateInterest_For30Days_ReturnsCorrectAmount() {
            // Arrange
            LocalDateTime endDate = startDate.plusDays(30);

            // Act
            BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

            // Assert
            assertThat(interest).isEqualByComparingTo(new BigDecimal("4.11"));
        }

        /**
         * Demonstrates: Boundary value testing (testing with minimum/edge values)
         * FIRST principles: Fast (simple calculation), Self-validating (clear zero expectation)
         * <p>
         * Tests the edge case where calculation period is zero days.
         * Boundary testing ensures the function handles edge cases correctly (min/max/zero values).
         * Complements threshold testing from AccountServiceTest - here we test mathematical boundaries.
         */
        @Test
        void calculateInterest_ForZeroDays_ReturnsZero() {
            // Arrange
            LocalDateTime endDate = startDate;

            // Act
            BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

            // Assert
            assertThat(interest).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * Demonstrates: Parametrized testing with @MethodSource for data-driven tests
         * FIRST principles: Fast (no I/O), Repeatable (same inputs always produce same outputs)
         * <p>
         * Tests interest calculation for different account types using parametrized approach.
         * One test method validates multiple scenarios with different input combinations.
         * Data-driven testing reduces code duplication and makes test data explicit.
         * <p>
         * Each account type has different annual interest rate:
         * - SAVINGS: 5% annually = 50.00 for 1000 balance over 365 days
         * - CHECKING: 1% annually = 10.00 for 1000 balance over 365 days
         * - BUSINESS: 3% annually = 30.00 for 1000 balance over 365 days
         */
        @ParameterizedTest
        @MethodSource("provideAccountTypesAndExpectedRates")
        void calculateInterest_ForDifferentAccountTypes_ReturnsExpectedInterest(
                AccountType accountType, BigDecimal expectedInterestFor365Days) {
            // Arrange
            savingsAccount.setType(accountType);
            LocalDateTime endDate = startDate.plusDays(365);

            // Act
            BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

            // Assert
            assertThat(interest).isEqualByComparingTo(expectedInterestFor365Days);
        }

        /**
         * Provides test data for parametrized test.
         * Each Arguments entry contains: (AccountType, expected interest for 365 days)
         */
        private static Stream<Arguments> provideAccountTypesAndExpectedRates() {
            return Stream.of(
                    Arguments.of(AccountType.SAVINGS, new BigDecimal("50.00")),
                    Arguments.of(AccountType.CHECKING, new BigDecimal("10.00")),
                    Arguments.of(AccountType.BUSINESS, new BigDecimal("30.00"))
            );
        }

    }

    /**
     * Optional test cases for InterestCalculator - additional practice scenarios.
     * Copy this entire class and paste it inside InterestCalculatorTest as a nested class named OptionalPart.
     */
    @Nested
    class OptionalPart {

        /**
         * Demonstrates: Testing with minimal time period (1 day)
         * FIRST principles: Fast (pure calculation), Repeatable (deterministic)
         * <p>
         * Tests interest calculation for the smallest meaningful period - 1 day.
         * This is a boundary test for time period - ensures daily interest works correctly.
         * For SAVINGS (5% annual): 1000 * 0.05 / 365 * 1 = 0.14 (rounded to 2 decimals)
         */
        @Test
        void calculateInterest_ForOneDay_ReturnsCorrectAmount() {
            // Arrange
            LocalDateTime endDate = startDate.plusDays(1);

            // Act
            BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

            // Assert
            assertThat(interest).isEqualByComparingTo(new BigDecimal("0.14"));
        }

        /**
         * Demonstrates: Testing with medium time period (90 days / ~3 months)
         * FIRST principles: Fast, Independent
         * <p>
         * Tests interest calculation for a quarterly period.
         * 90 days is a common financial period (quarter year).
         * For SAVINGS (5% annual): 1000 * 0.05 / 365 * 90 = 12.33
         */
        @Test
        void calculateInterest_For90Days_ReturnsCorrectAmount() {
            // Arrange
            LocalDateTime endDate = startDate.plusDays(90);

            // Act
            BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

            // Assert
            assertThat(interest).isEqualByComparingTo(new BigDecimal("12.33"));
        }

        /**
         * Demonstrates: Testing with long time period (multiple years)
         * FIRST principles: Fast (no real time wait), Repeatable
         * <p>
         * Tests interest calculation over 2 years (730 days).
         * Verifies the formula works correctly for extended periods.
         * For SAVINGS (5% annual): 1000 * 0.05 / 365 * 730 = 100.14 (2 years of simple interest)
         */
        @Test
        void calculateInterest_ForMultipleYears_ReturnsCorrectAmount() {
            // Arrange
            LocalDateTime endDate = startDate.plusYears(2);

            // Act
            BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

            // Assert
            assertThat(interest).isEqualByComparingTo(new BigDecimal("100.14"));
        }

        /**
         * Demonstrates: Compound interest with monthly compounding
         * FIRST principles: Fast, Independent
         * <p>
         * Tests compound interest calculated monthly (12 periods per year).
         * Compound interest formula: P * (1 + r/n)^(n*t) - P
         * where P=principal, r=annual rate, n=compounding periods/year, t=years
         * Monthly compounding is common for savings accounts.
         */
        @Test
        void calculateCompoundInterest_WithMonthlyCompounding_ReturnsCorrectAmount() {
            // Arrange
            LocalDateTime endDate = startDate.plusYears(1);
            int monthlyCompounding = 12;

            // Act
            BigDecimal interest = calculator.calculateCompoundInterest(
                    savingsAccount, startDate, endDate, monthlyCompounding);

            // Assert
            // For 1000 at 5% annual, compounded monthly for 1 year: ~51.16
            assertThat(interest).isEqualByComparingTo(new BigDecimal("51.16"));
        }

        /**
         * Demonstrates: Compound interest with quarterly compounding
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests compound interest calculated quarterly (4 periods per year).
         * Quarterly compounding is common for certificates of deposit (CDs).
         * Should yield more than simple interest but less than monthly compounding.
         */
        @Test
        void calculateCompoundInterest_WithQuarterlyCompounding_ReturnsCorrectAmount() {
            // Arrange
            LocalDateTime endDate = startDate.plusYears(1);
            int quarterlyCompounding = 4;

            // Act
            BigDecimal interest = calculator.calculateCompoundInterest(
                    savingsAccount, startDate, endDate, quarterlyCompounding);

            // Assert
            // For 1000 at 5% annual, compounded quarterly for 1 year: ~50.95
            assertThat(interest).isEqualByComparingTo(new BigDecimal("50.95"));
        }

        /**
         * Demonstrates: Compound interest with daily compounding
         * FIRST principles: Fast, Independent
         * <p>
         * Tests compound interest with daily compounding (365 periods per year).
         * Daily compounding yields the highest return for same annual rate.
         * This approaches continuous compounding as period count increases.
         */
        @Test
        void calculateCompoundInterest_WithDailyCompounding_ReturnsCorrectAmount() {
            // Arrange
            LocalDateTime endDate = startDate.plusYears(1);
            int dailyCompounding = 365;

            // Act
            BigDecimal interest = calculator.calculateCompoundInterest(
                    savingsAccount, startDate, endDate, dailyCompounding);

            // Assert
            // For 1000 at 5% annual, compounded daily for 1 year: ~51.27
            assertThat(interest).isEqualByComparingTo(new BigDecimal("51.27"));
        }

        /**
         * Demonstrates: Null account parameter validation
         * FIRST principles: Fast (immediate validation), Self-validating
         * <p>
         * Tests that null account is rejected before calculation.
         * All parameters must be validated - null account would cause NullPointerException.
         * Defensive programming: validate inputs before using them.
         */
        @Test
        void calculateInterest_WithNullAccount_ThrowsException() {
            // Arrange
            Account nullAccount = null;
            LocalDateTime endDate = startDate.plusDays(30);

            // Act & Assert
            assertThatThrownBy(() -> calculator.calculateInterest(nullAccount, startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account and dates cannot be null");
        }

        /**
         * Demonstrates: Null start date validation
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that null start date is rejected.
         * Both dates are required for period calculation - null would cause errors.
         */
        @Test
        void calculateInterest_WithNullFromDate_ThrowsException() {
            // Arrange
            LocalDateTime nullFromDate = null;
            LocalDateTime toDate = startDate.plusDays(30);

            // Act & Assert
            assertThatThrownBy(() -> calculator.calculateInterest(savingsAccount, nullFromDate, toDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account and dates cannot be null");
        }

        /**
         * Demonstrates: Null end date validation
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that null end date is rejected.
         * Complements null start date test - both dates must be present.
         */
        @Test
        void calculateInterest_WithNullToDate_ThrowsException() {
            // Arrange
            LocalDateTime nullToDate = null;

            // Act & Assert
            assertThatThrownBy(() -> calculator.calculateInterest(savingsAccount, startDate, nullToDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account and dates cannot be null");
        }

        /**
         * Demonstrates: Invalid date range validation (end before start)
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that reversed date range is rejected.
         * Business rule: end date must not be before start date.
         * This would result in negative time period, which is invalid.
         */
        @Test
        void calculateInterest_WithEndDateBeforeStartDate_ThrowsException() {
            // Arrange
            LocalDateTime endDate = startDate.minusDays(1);

            // Act & Assert
            assertThatThrownBy(() -> calculator.calculateInterest(savingsAccount, startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date cannot be before start date");
        }

        /**
         * Demonstrates: Edge case - interest calculation with zero balance
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that zero balance account generates zero interest.
         * Mathematical property: 0 * rate * time = 0
         * Important edge case - empty accounts shouldn't accrue interest.
         */
        @Test
        void calculateInterest_WithZeroBalance_ReturnsZero() {
            // Arrange
            savingsAccount.setBalance(BigDecimal.ZERO);
            LocalDateTime endDate = startDate.plusDays(365);

            // Act
            BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

            // Assert
            assertThat(interest).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * Demonstrates: Edge case - compound interest with zero balance
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that zero balance generates zero compound interest.
         * Complements simple interest zero balance test.
         * Both calculation methods should handle zero balance correctly.
         */
        @Test
        void calculateCompoundInterest_WithZeroBalance_ReturnsZero() {
            // Arrange
            savingsAccount.setBalance(BigDecimal.ZERO);
            LocalDateTime endDate = startDate.plusDays(365);
            int monthlyCompounding = 12;

            // Act
            BigDecimal interest = calculator.calculateCompoundInterest(
                    savingsAccount, startDate, endDate, monthlyCompounding);

            // Assert
            assertThat(interest).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * Demonstrates: Edge case - compound interest with zero compounding periods
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that zero or negative compounding periods are rejected.
         * Business rule: must have at least 1 compounding period per year.
         * Zero periods would cause division by zero.
         */
        @Test
        void calculateCompoundInterest_WithZeroCompoundingPeriods_ThrowsException() {
            // Arrange
            LocalDateTime endDate = startDate.plusYears(1);
            int zeroCompounding = 0;

            // Act & Assert
            assertThatThrownBy(() -> calculator.calculateCompoundInterest(
                    savingsAccount, startDate, endDate, zeroCompounding))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Compounding periods must be positive");
        }

        /**
         * Demonstrates: Edge case - compound interest with negative compounding periods
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that negative compounding periods are rejected.
         * Negative periods are mathematically invalid for interest calculation.
         */
        @Test
        void calculateCompoundInterest_WithNegativeCompoundingPeriods_ThrowsException() {
            // Arrange
            LocalDateTime endDate = startDate.plusYears(1);
            int negativeCompounding = -5;

            // Act & Assert
            assertThatThrownBy(() -> calculator.calculateCompoundInterest(
                    savingsAccount, startDate, endDate, negativeCompounding))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Compounding periods must be positive");
        }

        /**
         * Demonstrates: Boundary test - compound interest with zero days
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that compound interest for zero day period returns zero.
         * Same as simple interest - no time elapsed means no interest accrued.
         */
        @Test
        void calculateCompoundInterest_ForZeroDays_ReturnsZero() {
            // Arrange
            LocalDateTime endDate = startDate;
            int monthlyCompounding = 12;

            // Act
            BigDecimal interest = calculator.calculateCompoundInterest(
                    savingsAccount, startDate, endDate, monthlyCompounding);

            // Assert
            assertThat(interest).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * Demonstrates: Null account type validation in getAnnualRate
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that null account type is rejected when retrieving annual rate.
         * Each account type has specific rate - null type is invalid.
         */
        @Test
        void getAnnualRate_WithNullAccountType_ThrowsException() {
            // Arrange
            AccountType nullType = null;

            // Act & Assert
            assertThatThrownBy(() -> calculator.getAnnualRate(nullType))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account type cannot be null");
        }

        /**
         * Demonstrates: Verification of annual rates for all account types
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that getAnnualRate returns correct rates for each account type.
         * Verifies business rules: SAVINGS=5%, CHECKING=1%, BUSINESS=3%.
         * This is configuration testing - ensures rates match business requirements.
         */
        @Test
        void getAnnualRate_ReturnsCorrectRatesForAllAccountTypes() {
            // Act & Assert
            assertThat(calculator.getAnnualRate(AccountType.SAVINGS))
                    .isEqualByComparingTo(new BigDecimal("0.05"));
            assertThat(calculator.getAnnualRate(AccountType.CHECKING))
                    .isEqualByComparingTo(new BigDecimal("0.01"));
            assertThat(calculator.getAnnualRate(AccountType.BUSINESS))
                    .isEqualByComparingTo(new BigDecimal("0.03"));
        }
    }
}