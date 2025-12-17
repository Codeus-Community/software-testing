package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.exception.AccountBlockedException;
import org.codeus.unit_test.exception.InsufficientFundsException;
import org.codeus.unit_test.exception.InvalidTransactionException;
import org.codeus.unit_test.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransactionValidatorTest {

    private TransactionValidator validator;
    private Account activeAccount;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator();
        activeAccount = Account.builder()
                .id("acc-001")
                .clientId("client-001")
                .type(AccountType.CHECKING)
                .currency(Currency.USD)
                .balance(new BigDecimal("1000"))
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal("5000"))
                .build();
    }

    @Test
    void validateWithdrawal_WithSufficientFunds_DoesNotThrowException() {
        // Arrange
        BigDecimal amount = new BigDecimal("500");

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateWithdrawal(activeAccount, amount));
    }

    @Test
    void validateWithdrawal_WithInsufficientFunds_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("1500");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateWithdrawal(activeAccount, amount))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void validateWithdrawal_WithNegativeAmount_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("-100");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateWithdrawal(activeAccount, amount))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void validateWithdrawal_WithZeroAmount_ThrowsException() {
        // Arrange
        BigDecimal amount = BigDecimal.ZERO;

        // Act & Assert
        assertThatThrownBy(() -> validator.validateWithdrawal(activeAccount, amount))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void validateWithdrawal_WithNullAmount_ThrowsException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> validator.validateWithdrawal(activeAccount, null))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void validateWithdrawal_WithBlockedAccount_ThrowsException() {
        // Arrange
        activeAccount.setStatus(AccountStatus.BLOCKED);
        BigDecimal amount = new BigDecimal("100");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateWithdrawal(activeAccount, amount))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    void validateWithdrawal_WithClosedAccount_ThrowsException() {
        // Arrange
        activeAccount.setStatus(AccountStatus.CLOSED);
        BigDecimal amount = new BigDecimal("100");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateWithdrawal(activeAccount, amount))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void validateWithdrawal_WithNullAccount_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("100");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateWithdrawal(null, amount))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void validateDeposit_WithValidData_DoesNotThrowException() {
        // Arrange
        BigDecimal amount = new BigDecimal("500");

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDeposit(activeAccount, amount));
    }

    @Test
    void validateDeposit_WithBlockedAccount_ThrowsException() {
        // Arrange
        activeAccount.setStatus(AccountStatus.BLOCKED);
        BigDecimal amount = new BigDecimal("500");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateDeposit(activeAccount, amount))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    void validateTransfer_WithValidData_DoesNotThrowException() {
        // Arrange
        Account toAccount = Account.builder()
                .id("acc-002")
                .clientId("client-002")
                .type(AccountType.SAVINGS)
                .currency(Currency.USD)
                .balance(new BigDecimal("500"))
                .status(AccountStatus.ACTIVE)
                .build();
        BigDecimal amount = new BigDecimal("300");

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateTransfer(activeAccount, toAccount, amount));
    }

    @Test
    void validateTransfer_ToSameAccount_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("100");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateTransfer(activeAccount, activeAccount, amount))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void validateTransfer_WithInsufficientFunds_ThrowsException() {
        // Arrange
        Account toAccount = Account.builder()
                .id("acc-002")
                .status(AccountStatus.ACTIVE)
                .build();
        BigDecimal amount = new BigDecimal("1500");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateTransfer(activeAccount, toAccount, amount))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void validateDailyLimit_WithinLimit_DoesNotThrowException() {
        // Arrange
        BigDecimal amount = new BigDecimal("1000");
        BigDecimal alreadyWithdrawn = new BigDecimal("2000");

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDailyLimit(activeAccount, amount, alreadyWithdrawn));
    }

    @Test
    void validateDailyLimit_ExceedsLimit_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("3000");
        BigDecimal alreadyWithdrawn = new BigDecimal("3000");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateDailyLimit(activeAccount, amount, alreadyWithdrawn))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void validateDailyLimit_ExactlyAtLimit_DoesNotThrowException() {
        // Arrange
        BigDecimal amount = new BigDecimal("2000");
        BigDecimal alreadyWithdrawn = new BigDecimal("3000");

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateDailyLimit(activeAccount, amount, alreadyWithdrawn));
    }
}