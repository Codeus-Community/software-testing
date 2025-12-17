package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.enums.TransactionType;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleFraudDetectionServiceTest {

    private SimpleFraudDetectionService fraudService;
    private Account regularAccount;
    private Account businessAccount;

    @BeforeEach
    void setUp() {
        fraudService = new SimpleFraudDetectionService();

        regularAccount = Account.builder()
                .id("acc-001")
                .clientId("client-001")
                .type(AccountType.CHECKING)
                .currency(Currency.USD)
                .balance(new BigDecimal("5000"))
                .status(AccountStatus.ACTIVE)
                .build();

        businessAccount = Account.builder()
                .id("acc-002")
                .clientId("client-002")
                .type(AccountType.BUSINESS)
                .currency(Currency.USD)
                .balance(new BigDecimal("20000"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void isSuspicious_WithSmallAmount_ReturnsFalse() {
        // Arrange
        BigDecimal amount = new BigDecimal("1000");

        // Act
        boolean result = fraudService.isSuspicious(regularAccount, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSuspicious_WithAmountAboveThreshold_ReturnsTrue() {
        // Arrange
        BigDecimal amount = new BigDecimal("15000");

        // Act
        boolean result = fraudService.isSuspicious(regularAccount, amount);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isSuspicious_ForBusinessAccount_WithAmountAboveBusinessThreshold_ReturnsTrue() {
        // Arrange
        BigDecimal amount = new BigDecimal("60000");

        // Act
        boolean result = fraudService.isSuspicious(businessAccount, amount);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isSuspicious_ForBusinessAccount_WithAmountBelowBusinessThreshold_ReturnsFalse() {
        // Arrange
        BigDecimal amount = new BigDecimal("40000");

        // Act
        boolean result = fraudService.isSuspicious(businessAccount, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSuspicious_WithAmountFiveTimesBalance_ReturnsTrue() {
        // Arrange
        BigDecimal amount = new BigDecimal("30000");

        // Act
        boolean result = fraudService.isSuspicious(regularAccount, amount);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isSuspicious_WithAmountExactlyFiveTimesBalance_ReturnsTrue() {
        // Arrange
        BigDecimal amount = new BigDecimal("25000");

        // Act
        boolean result = fraudService.isSuspicious(regularAccount, amount);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isSuspicious_WithAmountLessThanFiveTimesBalance_ReturnsFalse() {
        // Arrange
        BigDecimal amount = new BigDecimal("9000");

        // Act
        boolean result = fraudService.isSuspicious(regularAccount, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSuspicious_WithZeroBalance_ReturnsFalse() {
        // Arrange
        regularAccount.setBalance(BigDecimal.ZERO);
        BigDecimal amount = new BigDecimal("5000");

        // Act
        boolean result = fraudService.isSuspicious(regularAccount, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"9999", "10000", "10001", "15000", "50000"})
    void isSuspicious_WithVariousAmounts_WorksCorrectly(String amountStr) {
        // Arrange
        BigDecimal amount = new BigDecimal(amountStr);

        // Act
        boolean result = fraudService.isSuspicious(regularAccount, amount);

        // Assert
        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            assertThat(result).isTrue();
        }
    }

    @Test
    void isSuspicious_WithNullAccount_ReturnsFalse() {
        // Arrange
        BigDecimal amount = new BigDecimal("5000");

        // Act
        boolean result = fraudService.isSuspicious(null, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSuspicious_WithNullAmount_ReturnsFalse() {
        // Act
        boolean result = fraudService.isSuspicious(regularAccount, null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSuspiciousTransfer_WithNormalTransfer_ReturnsFalse() {
        // Arrange
        Account toAccount = Account.builder()
                .id("acc-003")
                .currency(Currency.USD)
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("3000"))
                .build();
        BigDecimal amount = new BigDecimal("2000");

        // Act
        boolean result = fraudService.isSuspiciousTransfer(regularAccount, toAccount, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSuspiciousTransfer_WithLargeAmount_ReturnsTrue() {
        // Arrange
        Account toAccount = Account.builder()
                .id("acc-003")
                .currency(Currency.USD)
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("3000"))
                .build();
        BigDecimal amount = new BigDecimal("15000");

        // Act
        boolean result = fraudService.isSuspiciousTransfer(regularAccount, toAccount, amount);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isSuspiciousTransfer_WithCurrencyChangeLargeAmount_ReturnsTrue() {
        // Arrange
        Account toAccount = Account.builder()
                .id("acc-003")
                .currency(Currency.EUR)
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("3000"))
                .build();
        BigDecimal amount = new BigDecimal("6000");

        // Act
        boolean result = fraudService.isSuspiciousTransfer(regularAccount, toAccount, amount);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isSuspiciousTransfer_WithCurrencyChangeSmallAmount_ReturnsFalse() {
        // Arrange
        Account toAccount = Account.builder()
                .id("acc-003")
                .currency(Currency.EUR)
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("3000"))
                .build();
        BigDecimal amount = new BigDecimal("2000");

        // Act
        boolean result = fraudService.isSuspiciousTransfer(regularAccount, toAccount, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "USD, USD, 3000, false",
            "USD, EUR, 3000, false",
            "USD, EUR, 6000, true",
            "EUR, UAH, 8000, true",
            "USD, USD, 15000, true"
    })
    void isSuspiciousTransfer_WithDifferentScenarios_ReturnsExpectedResult(
            Currency fromCurrency, Currency toCurrency, String amountStr, boolean expectedSuspicious) {
        // Arrange
        regularAccount.setCurrency(fromCurrency);
        Account toAccount = Account.builder()
                .id("acc-003")
                .currency(toCurrency)
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("3000"))
                .build();
        BigDecimal amount = new BigDecimal(amountStr);

        // Act
        boolean result = fraudService.isSuspiciousTransfer(regularAccount, toAccount, amount);

        // Assert
        assertThat(result).isEqualTo(expectedSuspicious);
    }

    @Test
    void isSuspiciousTransfer_WithNullFromAccount_ReturnsFalse() {
        // Arrange
        Account toAccount = Account.builder()
                .id("acc-003")
                .currency(Currency.USD)
                .build();
        BigDecimal amount = new BigDecimal("5000");

        // Act
        boolean result = fraudService.isSuspiciousTransfer(null, toAccount, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSuspiciousTransfer_WithNullToAccount_ReturnsFalse() {
        // Arrange
        BigDecimal amount = new BigDecimal("5000");

        // Act
        boolean result = fraudService.isSuspiciousTransfer(regularAccount, null, amount);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSuspiciousTransfer_WithNullAmount_ReturnsFalse() {
        // Arrange
        Account toAccount = Account.builder()
                .id("acc-003")
                .currency(Currency.USD)
                .build();

        // Act
        boolean result = fraudService.isSuspiciousTransfer(regularAccount, toAccount, null);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void reportSuspiciousActivity_WithTransaction_PrintsMessage() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id("txn-001")
                .fromAccountId("acc-001")
                .toAccountId("acc-002")
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("15000"))
                .currency(Currency.USD)
                .timestamp(LocalDateTime.now())
                .build();

        // Act & Assert (just verify it doesn't throw exception)
        fraudService.reportSuspiciousActivity(transaction);
    }
}