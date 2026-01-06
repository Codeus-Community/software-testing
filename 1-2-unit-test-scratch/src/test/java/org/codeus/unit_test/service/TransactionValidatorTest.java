package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.exception.InsufficientFundsException;
import org.codeus.unit_test.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransactionValidatorTest {

    private TransactionValidator validator;
    private Account activeAccount;

    /**
     * Setup executed before each test.
     * Initializes validator and account with sufficient balance.
     */
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

    /**
     * Demonstrates: Validating happy path with assertDoesNotThrow()
     * FIRST principles: Fast (no dependencies), Independent (pure validation)
     *
     * Tests that valid withdrawal passes validation without throwing exceptions.
     * Uses assertDoesNotThrow() to verify no exceptions occur - different from
     * verify() used in mocked tests or assertThat() used for value assertions.
     *
     * This is a pure validator test - no mocks needed, just business logic validation.
     */
    @Test
    void validateWithdrawal_WithSufficientFunds_DoesNotThrowException() {
        // Arrange
        BigDecimal amount = new BigDecimal("500");

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateWithdrawal(activeAccount, amount));
    }

    /**
     * Demonstrates: Testing business rule violations with specific exception types
     * FIRST principles: Self-validating (clear exception expected)
     *
     * Tests that insufficient funds are properly detected and rejected.
     * Shows InsufficientFundsException - a domain-specific exception type
     * that represents a business rule violation (not a technical error).
     *
     * Validator classes throw multiple exception types to distinguish different
     * failure reasons - this helps calling code handle errors appropriately.
     */
    @Test
    void validateWithdrawal_WithInsufficientFunds_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("1500");

        // Act & Assert
        assertThatThrownBy(() -> validator.validateWithdrawal(activeAccount, amount))
                .isInstanceOf(InsufficientFundsException.class);
    }
}