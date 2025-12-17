package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
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

    @Test
    void calculateInterest_For30Days_ReturnsCorrectAmount() {
        // Arrange
        LocalDateTime endDate = startDate.plusDays(30);

        // Act
        BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

        // Assert
        assertThat(interest).isEqualByComparingTo(new BigDecimal("4.11"));
    }

    @Test
    void calculateInterest_For365Days_ReturnsCorrectAmount() {
        // Arrange
        LocalDateTime endDate = startDate.plusDays(365);

        // Act
        BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

        // Assert
        assertThat(interest).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void calculateInterest_ForZeroDays_ReturnsZero() {
        // Arrange
        LocalDateTime endDate = startDate;

        // Act
        BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

        // Assert
        assertThat(interest).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @EnumSource(AccountType.class)
    void calculateInterest_ForDifferentAccountTypes_ReturnsPositiveInterest(AccountType accountType) {
        // Arrange
        savingsAccount.setType(accountType);
        LocalDateTime endDate = startDate.plusDays(365);

        // Act
        BigDecimal interest = calculator.calculateInterest(savingsAccount, startDate, endDate);

        // Assert
        assertThat(interest).isGreaterThan(BigDecimal.ZERO);
    }

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

    @Test
    void calculateInterest_WithNullAccount_ThrowsException() {
        // Arrange
        LocalDateTime endDate = startDate.plusDays(30);

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculateInterest(null, startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateInterest_WithNullFromDate_ThrowsException() {
        // Arrange
        LocalDateTime endDate = startDate.plusDays(30);

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculateInterest(savingsAccount, null, endDate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateInterest_WithNullToDate_ThrowsException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> calculator.calculateInterest(savingsAccount, startDate, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateInterest_WithEndDateBeforeStartDate_ThrowsException() {
        // Arrange
        LocalDateTime endDate = startDate.minusDays(10);

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculateInterest(savingsAccount, startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateCompoundInterest_WithQuarterlyCompounding_ReturnsCorrectAmount() {
        // Arrange
        LocalDateTime endDate = startDate.plusDays(365);
        int quarterlyPeriods = 4;

        // Act
        BigDecimal interest = calculator.calculateCompoundInterest(
                savingsAccount, startDate, endDate, quarterlyPeriods);

        // Assert
        assertThat(interest).isGreaterThan(new BigDecimal("50.00"));
    }

    @Test
    void calculateCompoundInterest_WithZeroDays_ReturnsZero() {
        // Arrange
        LocalDateTime endDate = startDate;

        // Act
        BigDecimal interest = calculator.calculateCompoundInterest(
                savingsAccount, startDate, endDate, 12);

        // Assert
        assertThat(interest).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateCompoundInterest_WithNegativePeriods_ThrowsException() {
        // Arrange
        LocalDateTime endDate = startDate.plusDays(365);

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculateCompoundInterest(
                savingsAccount, startDate, endDate, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateCompoundInterest_WithZeroPeriods_ThrowsException() {
        // Arrange
        LocalDateTime endDate = startDate.plusDays(365);

        // Act & Assert
        assertThatThrownBy(() -> calculator.calculateCompoundInterest(
                savingsAccount, startDate, endDate, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAnnualRate_ForSavingsAccount_ReturnsCorrectRate() {
        // Arrange & Act
        BigDecimal rate = calculator.getAnnualRate(AccountType.SAVINGS);

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.05"));
    }

    @Test
    void getAnnualRate_ForCheckingAccount_ReturnsCorrectRate() {
        // Arrange & Act
        BigDecimal rate = calculator.getAnnualRate(AccountType.CHECKING);

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    void getAnnualRate_ForBusinessAccount_ReturnsCorrectRate() {
        // Arrange & Act
        BigDecimal rate = calculator.getAnnualRate(AccountType.BUSINESS);

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.03"));
    }

    @Test
    void getAnnualRate_WithNull_ThrowsException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> calculator.getAnnualRate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> provideAccountTypesAndExpectedRates() {
        return Stream.of(
                Arguments.of(AccountType.SAVINGS, new BigDecimal("50.00")),
                Arguments.of(AccountType.CHECKING, new BigDecimal("10.00")),
                Arguments.of(AccountType.BUSINESS, new BigDecimal("30.00"))
        );
    }
}