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
import org.junit.jupiter.api.Nested;
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

    @Nested
    class MainPart {
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

    /**
     * Optional test cases for TransactionService - additional practice scenarios.
     * Copy this entire class and paste it inside TransactionServiceTest as a nested class named OptionalPart.
     */
    @Nested
    class OptionalPart {

        private ExchangeRateService exchangeRateService;

        @BeforeEach
        void setUpOptional() {
            exchangeRateService = mock(ExchangeRateService.class);
            transactionService = new TransactionService(
                    accountRepository,
                    transactionRepository,
                    transactionValidator,
                    fraudDetectionService,
                    notificationService,
                    exchangeRateService,
                    timeProvider
            );
        }

        /**
         * Demonstrates: Cross-currency transfer with exchange rate service
         * FIRST principles: Fast (mocked exchange service), Independent
         * <p>
         * Tests transfer between accounts with different currencies.
         * From: USD account, To: EUR account
         * Service must convert amount using exchange rate: 1000 USD * 0.92 = 920 EUR
         * This tests integration with ExchangeRateService.
         */
        @Test
        void transfer_BetweenDifferentCurrencies_ConvertsAmountCorrectly() {
            // Arrange
            BigDecimal amount = new BigDecimal("1000");
            toAccount.setCurrency(Currency.EUR);

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
            when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
            when(exchangeRateService.convert(any(), any(), any()))
                    .thenReturn(new BigDecimal("920.00")); // 1000 USD -> 920 EUR
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            transactionService.transfer("acc-001", "acc-002", amount);

            // Assert
            verify(exchangeRateService).convert(amount, Currency.USD, Currency.EUR);
            assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("3990")); // 5000 - 1000 - 10 (fee)
            assertThat(toAccount.getBalance()).isEqualByComparingTo(new BigDecimal("2920.00")); // 2000 + 920 (converted)
        }

        /**
         * Demonstrates: Multiple withdrawals within daily limit
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that multiple withdrawals are tracked for daily limit.
         * Tests scenario: already withdrawn 7500, trying to withdraw 1500 more.
         * Total: 9000, which is within 10000 limit but triggers warning (90% threshold).
         */
        /**
         * Demonstrates: Multiple withdrawals within daily limit
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that multiple withdrawals are tracked for daily limit.
         * Tests scenario: already withdrawn 7600, trying to withdraw 1500 more.
         * Total: 9100, which is within 10000 limit but triggers warning (>90% threshold).
         */
        @Test
        void withdraw_MultipleWithdrawalsTrackingDailyLimit_AllowsWithinLimit() {
            // Arrange
            BigDecimal amount = new BigDecimal("1500");
            fromAccount.setDailyWithdrawalLimit(new BigDecimal("10000"));

            Transaction existingWithdrawal1 = Transaction.builder()
                    .type(TransactionType.WITHDRAWAL)
                    .fromAccountId("acc-001")
                    .amount(new BigDecimal("5100"))
                    .build();

            Transaction existingWithdrawal2 = Transaction.builder()
                    .type(TransactionType.WITHDRAWAL)
                    .fromAccountId("acc-001")
                    .amount(new BigDecimal("2500"))
                    .build();

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
                    .thenReturn(List.of(existingWithdrawal1, existingWithdrawal2));

            // Act
            Transaction result = transactionService.withdraw("acc-001", amount, "Withdrawal");

            // Assert
            assertThat(result).isNotNull();
            // 5100 + 2500 + 1500 = 9100 > 9000 (90% of 10000), triggers warning
            verify(notificationService).sendDailyLimitWarning("client-001", "acc-001");
        }

        /**
         * Demonstrates: Fee calculation for different transfer amounts
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that transfer fee is calculated correctly based on amount.
         * Fee = 1% of amount, min 1, max 50
         * For 500: fee = 5.00 (within min-max range)
         */
        @Test
        void transfer_CalculatesFeeCorrectly_ForMediumAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("500");

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
            when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Transaction result = transactionService.transfer("acc-001", "acc-002", amount);

            // Assert
            assertThat(result.getFee()).isEqualByComparingTo(new BigDecimal("5.00")); // 1% of 500
            assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("4495")); // 5000 - 500 - 5
        }

        /**
         * Demonstrates: Fee calculation minimum threshold
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that minimum fee is applied for small transfers.
         * For 50: 1% would be 0.50, but min fee is 1.00
         * So fee should be 1.00 (the minimum)
         */
        @Test
        void transfer_AppliesMinimumFee_ForSmallAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("50");

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
            when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Transaction result = transactionService.transfer("acc-001", "acc-002", amount);

            // Assert
            assertThat(result.getFee()).isEqualByComparingTo(new BigDecimal("1.00")); // MIN_FEE
            assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("4949")); // 5000 - 50 - 1
        }

        /**
         * Demonstrates: Fee calculation maximum threshold
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that maximum fee cap is applied for large transfers.
         * For 10000: 1% would be 100, but max fee is 50
         * So fee should be 50.00 (the maximum)
         */
        @Test
        void transfer_AppliesMaximumFee_ForLargeAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("10000");
            fromAccount.setBalance(new BigDecimal("15000"));

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
            when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Transaction result = transactionService.transfer("acc-001", "acc-002", amount);

            // Assert
            assertThat(result.getFee()).isEqualByComparingTo(new BigDecimal("50.00")); // MAX_FEE
            assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("4950")); // 15000 - 10000 - 50
        }

        /**
         * Demonstrates: Deposit operation with description
         * FIRST principles: Fast, Independent
         * <p>
         * Tests deposit method which is separate from AccountService.deposit.
         * TransactionService.deposit also records transaction with description.
         */
        @Test
        void deposit_WithValidData_CreatesTransaction() {
            // Arrange
            String accountId = "acc-001";
            BigDecimal amount = new BigDecimal("500");
            String description = "Salary deposit";

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(fromAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(timeProvider.now()).thenReturn(fixedTime);

            // Act
            Transaction result = transactionService.deposit(accountId, amount, description);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(result.getAmount()).isEqualByComparingTo(amount);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("5500")); // 5000 + 500
            verify(transactionRepository).save(any(Transaction.class));
            verify(notificationService).sendTransactionNotification(eq("client-001"), any(Transaction.class));
        }

        /**
         * Demonstrates: Withdrawal operation with description
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests withdrawal method in TransactionService.
         * Unlike AccountService.withdraw, this also creates transaction record.
         */
        @Test
        void withdraw_WithValidData_CreatesTransaction() {
            // Arrange
            String accountId = "acc-001";
            BigDecimal amount = new BigDecimal("500");
            String description = "ATM withdrawal";

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(fromAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
                    .thenReturn(List.of());
            when(timeProvider.now()).thenReturn(fixedTime);

            // Act
            Transaction result = transactionService.withdraw(accountId, amount, description);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(result.getAmount()).isEqualByComparingTo(amount);
            assertThat(result.getDescription()).isEqualTo(description);
            assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("4500")); // 5000 - 500
            verify(transactionRepository).save(any(Transaction.class));
        }

        /**
         * Demonstrates: Transaction history retrieval by account
         * FIRST principles: Fast, Independent
         * <p>
         * Tests getAccountTransactions method.
         * Verifies that service correctly delegates to repository.
         */
        @Test
        void getAccountTransactions_ReturnsAllTransactionsForAccount() {
            // Arrange
            String accountId = "acc-001";
            List<Transaction> expectedTransactions = List.of(
                    Transaction.builder().id("tx-1").fromAccountId(accountId).build(),
                    Transaction.builder().id("tx-2").toAccountId(accountId).build()
            );

            when(transactionRepository.findByAccountId(accountId)).thenReturn(expectedTransactions);

            // Act
            List<Transaction> result = transactionService.getAccountTransactions(accountId);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).isEqualTo(expectedTransactions);
            verify(transactionRepository).findByAccountId(accountId);
        }

        /**
         * Demonstrates: Transaction history retrieval by date range
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests getAccountTransactionsByDateRange method.
         * Verifies filtering transactions by date period.
         */
        @Test
        void getAccountTransactionsByDateRange_ReturnsFilteredTransactions() {
            // Arrange
            String accountId = "acc-001";
            LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2024, 1, 31, 23, 59);

            List<Transaction> expectedTransactions = List.of(
                    Transaction.builder().id("tx-1").timestamp(LocalDateTime.of(2024, 1, 15, 10, 0)).build()
            );

            when(transactionRepository.findByAccountIdAndDateRange(accountId, from, to))
                    .thenReturn(expectedTransactions);

            // Act
            List<Transaction> result = transactionService.getAccountTransactionsByDateRange(accountId, from, to);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result).isEqualTo(expectedTransactions);
            verify(transactionRepository).findByAccountIdAndDateRange(accountId, from, to);
        }

        /**
         * Demonstrates: Empty transaction history scenario
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that getAccountTransactions handles empty results correctly.
         * New accounts or accounts with no activity return empty list.
         */
        @Test
        void getAccountTransactions_WithNoTransactions_ReturnsEmptyList() {
            // Arrange
            String accountId = "acc-001";
            when(transactionRepository.findByAccountId(accountId)).thenReturn(List.of());

            // Act
            List<Transaction> result = transactionService.getAccountTransactions(accountId);

            // Assert
            assertThat(result).isEmpty();
            verify(transactionRepository).findByAccountId(accountId);
        }

        /**
         * Demonstrates: Transfer without currency conversion (same currency)
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that transfer between same-currency accounts doesn't call exchange service.
         * This is optimization - no conversion needed when currencies match.
         */
        @Test
        void transfer_WithSameCurrency_DoesNotCallExchangeService() {
            // Arrange
            BigDecimal amount = new BigDecimal("1000");

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
            when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            transactionService.transfer("acc-001", "acc-002", amount);

            // Assert
            verify(exchangeRateService, never()).convert(any(), any(), any());
            assertThat(toAccount.getBalance()).isEqualByComparingTo(new BigDecimal("3000")); // 2000 + 1000 (no conversion)
        }

        /**
         * Demonstrates: Both accounts receive notifications after transfer
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that transfer sends notifications to both sender and receiver.
         * Both parties need to be informed about the transaction.
         */
        @Test
        void transfer_SendsNotificationsToBothAccounts() {
            // Arrange
            BigDecimal amount = new BigDecimal("500");

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
            when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            transactionService.transfer("acc-001", "acc-002", amount);

            // Assert
            verify(notificationService).sendTransactionNotification(eq("client-001"), any(Transaction.class));
            verify(notificationService).sendTransactionNotification(eq("client-002"), any(Transaction.class));
            verify(notificationService, times(2)).sendTransactionNotification(anyString(), any(Transaction.class));
        }

        /**
         * Demonstrates: Daily limit warning not triggered when well below limit
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that warning is NOT sent when withdrawal total is below 90% threshold.
         * Already withdrawn: 5000, trying: 1000, total: 6000
         * Limit: 10000, 90% threshold: 9000
         * 6000 < 9000, so no warning should be sent
         */
        @Test
        void withdraw_WellBelowDailyLimit_DoesNotSendWarning() {
            // Arrange
            BigDecimal amount = new BigDecimal("1000");
            fromAccount.setDailyWithdrawalLimit(new BigDecimal("10000"));

            Transaction existingWithdrawal = Transaction.builder()
                    .type(TransactionType.WITHDRAWAL)
                    .fromAccountId("acc-001")
                    .amount(new BigDecimal("5000"))
                    .build();

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any()))
                    .thenReturn(List.of(existingWithdrawal));

            // Act
            transactionService.withdraw("acc-001", amount, "Withdrawal");

            // Assert
            verify(notificationService, never()).sendDailyLimitWarning(anyString(), anyString());
            verify(notificationService).sendTransactionNotification(eq("client-001"), any(Transaction.class));
        }

        /**
         * Demonstrates: Cross-currency transfer with rate verification
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that exchange service is called with correct parameters.
         * Verifies the amount, from currency, and to currency are passed correctly.
         */
        @Test
        void transfer_DifferentCurrencies_CallsExchangeServiceWithCorrectParams() {
            // Arrange
            BigDecimal amount = new BigDecimal("1000");
            toAccount.setCurrency(Currency.EUR);

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
            when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
            when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
            when(exchangeRateService.convert(amount, Currency.USD, Currency.EUR))
                    .thenReturn(new BigDecimal("920.00"));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            transactionService.transfer("acc-001", "acc-002", amount);

            // Assert
            verify(exchangeRateService).convert(
                    eq(amount),
                    eq(Currency.USD),
                    eq(Currency.EUR)
            );
        }
    }
}