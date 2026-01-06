package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.enums.TransactionType;
import org.codeus.unit_test.exception.FraudDetectedException;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.model.Transaction;
import org.codeus.unit_test.repository.AccountRepository;
import org.codeus.unit_test.repository.TransactionRepository;
import org.codeus.unit_test.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionValidator transactionValidator;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;
    private LocalDateTime fixedTime;

    /**
     * Setup executed before each test.
     * Initializes test accounts and fixed time for deterministic testing.
     */
    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.of(2024, 1, 15, 10, 0);

        fromAccount = Account.builder()
                .id("acc-001")
                .clientId("client-001")
                .type(AccountType.CHECKING)
                .currency(Currency.USD)
                .balance(new BigDecimal("5000"))
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal("10000"))
                .build();

        toAccount = Account.builder()
                .id("acc-002")
                .clientId("client-002")
                .type(AccountType.SAVINGS)
                .currency(Currency.USD)
                .balance(new BigDecimal("2000"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    /**
     * Demonstrates: Complex isolation with multiple dependencies working together
     * FIRST principles: Fast (all dependencies mocked), Independent (no real services)
     * <p>
     * Tests the complete transfer flow with multiple mocked services.
     * Shows how to isolate a service that depends on many other services.
     * Verifies the core business logic: balance changes and fee calculation.
     */
    @Test
    void transfer_WithValidData_TransfersMoneySuccessfully() {
        // Arrange
        BigDecimal amount = new BigDecimal("1000");

        when(timeProvider.now()).thenReturn(fixedTime);
        when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
        when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = transactionService.transfer("acc-001", "acc-002", amount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("3990")); // 5000 - 1000 - 10 (fee)
        assertThat(toAccount.getBalance()).isEqualByComparingTo(new BigDecimal("3000"));   // 2000 + 1000

        verify(transactionValidator).validateTransfer(fromAccount, toAccount, amount);
        verify(fraudDetectionService).isSuspiciousTransfer(fromAccount, toAccount, amount);
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    /**
     * Demonstrates: InOrder verification - ensuring operations happen in correct sequence
     * FIRST principles: Fast (no I/O), Self-validating (clear order verification)
     * <p>
     * Tests that transfer operations occur in the proper order:
     * 1. Validate transaction
     * 2. Check for fraud
     * 3. Update accounts
     * 4. Save transaction
     * 5. Send notifications
     * <p>
     * Order matters in financial operations - you can't save before validating!
     * InOrder ensures critical operations follow business rules sequence.
     */
    @Test
    void transfer_VerifiesCorrectOrderOfOperations() {
        // Arrange
        BigDecimal amount = new BigDecimal("500");

        when(timeProvider.now()).thenReturn(fixedTime);
        when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
        when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionService.transfer("acc-001", "acc-002", amount);

        // Assert - verify order
        InOrder inOrder = inOrder(transactionValidator, fraudDetectionService, accountRepository,
                transactionRepository, notificationService);
        inOrder.verify(transactionValidator).validateTransfer(fromAccount, toAccount, amount);
        inOrder.verify(fraudDetectionService).isSuspiciousTransfer(fromAccount, toAccount, amount);
        inOrder.verify(accountRepository).save(fromAccount);
        inOrder.verify(accountRepository).save(toAccount);
        inOrder.verify(transactionRepository).save(any(Transaction.class));
        inOrder.verify(notificationService).sendTransactionNotification(eq("client-001"), any(Transaction.class));
        inOrder.verify(notificationService).sendTransactionNotification(eq("client-002"), any(Transaction.class));
    }

    /**
     * Demonstrates: Negative verification with never() - ensuring operations DON'T happen
     * FIRST principles: Independent (isolated fraud scenario), Repeatable (deterministic fraud check)
     * <p>
     * Tests that when fraud is detected, no actual transfer occurs.
     * Uses never() to verify critical operations are skipped:
     * - Accounts are NOT saved
     * - Transaction is NOT recorded
     * - Notifications are NOT sent
     * <p>
     * This is crucial for security - fraudulent operations must be completely blocked.
     */
    @Test
    void transfer_WithSuspiciousActivity_DoesNotTransfer() {
        // Arrange
        BigDecimal amount = new BigDecimal("10000");

        when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
        when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(true);

        BigDecimal originalFromBalance = fromAccount.getBalance();
        BigDecimal originalToBalance = toAccount.getBalance();

        // Act & Assert
        assertThatThrownBy(() -> transactionService.transfer("acc-001", "acc-002", amount))
                .isInstanceOf(FraudDetectedException.class);

        verify(fraudDetectionService).reportSuspiciousActivity(any(Transaction.class));
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(notificationService, never()).sendTransactionNotification(anyString(), any());

        assertThat(fromAccount.getBalance()).isEqualByComparingTo(originalFromBalance);
        assertThat(toAccount.getBalance()).isEqualByComparingTo(originalToBalance);
    }

    /**
     * Demonstrates: Integration of multiple services, conditional notifications
     * FIRST principles: Fast (mocked time and repositories)
     * <p>
     * Tests withdrawal near daily limit triggers a warning notification.
     * Shows integration between transaction history, limit checking, and notifications.
     * Tests conditional logic: warning sent only when approaching limit (90%).
     */
    @Test
    void withdraw_NearDailyLimit_SendsWarning() {
        // Arrange
        BigDecimal amount = new BigDecimal("1600");
        fromAccount.setDailyWithdrawalLimit(new BigDecimal("10000"));

        Transaction existingWithdrawal = Transaction.builder()
                .type(TransactionType.WITHDRAWAL)
                .fromAccountId("acc-001")
                .amount(new BigDecimal("7500"))
                .build();

        when(timeProvider.now()).thenReturn(fixedTime);
        when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
                .thenReturn(List.of(existingWithdrawal));

        // Act
        transactionService.withdraw("acc-001", amount, "Withdrawal");

        // Assert - 7500 + 1600 = 9100 > 9000 (90% of 10000), triggers warning
        verify(notificationService).sendDailyLimitWarning("client-001", "acc-001");
    }
}