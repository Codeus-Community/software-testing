package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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

    /**
     * Demonstrates: Testing pure functions without any mocks or external dependencies
     * FIRST principles: Fast (pure calculation), Independent (no external state),
     *                   Repeatable (deterministic mathematical operation)
     *
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
     *
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
     *
     * Tests interest calculation for different account types using parametrized approach.
     * One test method validates multiple scenarios with different input combinations.
     * Data-driven testing reduces code duplication and makes test data explicit.
     *
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