package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.exception.FraudDetectedException;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.repository.AccountRepository;
import org.codeus.unit_test.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionValidator transactionValidator;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private AccountService accountService;

    private LocalDateTime fixedTime;

    /**
     * Test fixture setup - runs before each test method.
     * <p>
     * Initializes a fixed time to ensure test repeatability (FIRST - Repeatable).
     * Using a fixed time instead of LocalDateTime.now() makes tests deterministic
     * and independent of when they are executed.
     */
    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.of(2024, 1, 15, 10, 0);
    }

    private Account createAccount(String accountId, BigDecimal balance) {
        return Account.builder()
                .id(accountId)
                .clientId("client-001")
                .type(AccountType.CHECKING)
                .currency(Currency.USD)
                .balance(balance)
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal("10000"))
                .build();
    }

    @Nested
    class MainPart {
        /**
         * Demonstrates: AAA pattern (Arrange-Act-Assert), basic mocking, happy path testing
         * FIRST principles: Fast (no I/O), Independent (isolated test with mocks)
         * <p>
         * Tests the basic account creation flow with valid data.
         * Shows how to mock dependencies and verify the created account properties.
         */
        @Test
        void createAccount_WithValidData_CreatesAndReturnsAccount() {
            // Arrange
            String clientId = "client-001";
            AccountType type = AccountType.SAVINGS;
            Currency currency = Currency.USD;
            BigDecimal initialDeposit = new BigDecimal("1000");

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Account result = accountService.createAccount(clientId, type, currency, initialDeposit);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getClientId()).isEqualTo(clientId);
            assertThat(result.getType()).isEqualTo(type);
            assertThat(result.getCurrency()).isEqualTo(currency);
            assertThat(result.getBalance()).isEqualByComparingTo(initialDeposit);
            assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(result.getCreatedAt()).isEqualTo(fixedTime);
            assertThat(result.getDailyWithdrawalLimit()).isEqualByComparingTo(new BigDecimal("5000"));

            verify(accountRepository).save(any(Account.class));
            verify(timeProvider).now();
        }

        /**
         * Demonstrates: Mock verification with verify(), testing interactions between dependencies
         * FIRST principles: Fast (no real database), Self-validating (clear pass/fail)
         * <p>
         * Tests deposit operation and verifies that all dependencies are called correctly.
         * Shows how to verify mock interactions with validator, repository, and notification service.
         */
        @Test
        void deposit_WithValidData_IncreasesBalance() {
            // Arrange
            String accountId = "acc-001";
            BigDecimal initialBalance = new BigDecimal("1000");
            BigDecimal depositAmount = new BigDecimal("500");

            Account account = createAccount(accountId, initialBalance);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(fraudDetectionService.isSuspicious(any(Account.class), any(BigDecimal.class))).thenReturn(false);

            // Act
            Account result = accountService.deposit(accountId, depositAmount);

            // Assert
            assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("1500"));

            verify(transactionValidator).validateDeposit(account, depositAmount);
            verify(fraudDetectionService).isSuspicious(account, depositAmount);
            verify(accountRepository).save(account);
            verify(notificationService).sendTransactionNotification(eq(account.getClientId()), any());
        }

        /**
         * Demonstrates: Edge case testing, conditional logic (threshold), multiple mock verifications
         * FIRST principles: Fast, Independent (no shared state between tests)
         * <p>
         * Tests withdrawal that results in low balance and verifies that alert is sent.
         * Shows how to test edge cases where balance crosses a threshold (LOW_BALANCE_THRESHOLD).
         */
        @Test
        void withdraw_ResultingInLowBalance_SendsLowBalanceAlert() {
            // Arrange
            String accountId = "acc-001";
            BigDecimal initialBalance = new BigDecimal("150");
            BigDecimal withdrawAmount = new BigDecimal("100");

            Account account = createAccount(accountId, initialBalance);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(fraudDetectionService.isSuspicious(any(Account.class), any(BigDecimal.class))).thenReturn(false);

            // Act
            accountService.withdraw(accountId, withdrawAmount);

            // Assert
            verify(notificationService).sendLowBalanceAlert(account.getClientId(), accountId);
            verify(notificationService).sendTransactionNotification(eq(account.getClientId()), any());
        }

        /**
         * Demonstrates: Exception handling, testing negative scenarios, fraud detection integration
         * FIRST principles: Independent (test isolated behavior), Repeatable (deterministic fraud check)
         * <p>
         * Tests that suspicious deposits are detected and rejected with FraudDetectedException.
         * Shows how to test exception scenarios and verify that certain operations are NOT called.
         */
        @Test
        void deposit_WithSuspiciousAmount_ThrowsFraudException() {
            // Arrange
            String accountId = "acc-001";
            BigDecimal depositAmount = new BigDecimal("50000");
            Account account = createAccount(accountId, new BigDecimal("1000"));

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(fraudDetectionService.isSuspicious(any(Account.class), any(BigDecimal.class))).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> accountService.deposit(accountId, depositAmount))
                    .isInstanceOf(FraudDetectedException.class)
                    .hasMessageContaining("Suspicious deposit detected");

            verify(fraudDetectionService).isSuspicious(account, depositAmount);
            verify(accountRepository, never()).save(any(Account.class));
            verify(notificationService, never()).sendTransactionNotification(anyString(), any());
        }

        /**
         * Demonstrates: Business rule validation, negative testing with exceptions
         * FIRST principles: Self-validating (clear assertion on exception)
         * <p>
         * Tests that closing an account with positive balance is not allowed.
         * Shows how to test business rules and verify that invalid operations throw exceptions.
         */
        @Test
        void closeAccount_WithPositiveBalance_ThrowsException() {
            // Arrange
            String accountId = "acc-001";
            Account account = createAccount(accountId, new BigDecimal("100"));

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // Act & Assert
            assertThatThrownBy(() -> accountService.closeAccount(accountId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot close account with positive balance");

            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    /**
     * Optional test cases for AccountService - additional practice scenarios.
     */
    @Nested
    class OptionalPart {

        /**
         * Demonstrates: Parameterized testing for different account types
         * FIRST principles: Fast (no I/O), Repeatable (deterministic for each account type)
         * <p>
         * Tests that different account types are created with correct default daily limits.
         * Each account type has specific withdrawal limits based on business rules:
         * - SAVINGS: 5000 (conservative limit for savings protection)
         * - CHECKING: 10000 (higher limit for everyday transactions)
         * - BUSINESS: 50000 (highest limit for business operations)
         */
        @ParameterizedTest
        @MethodSource("provideAccountTypesAndLimits")
        void createAccount_WithDifferentAccountTypes_SetsCorrectDailyLimit(
                AccountType accountType, BigDecimal expectedLimit) {
            // Arrange
            String clientId = "client-001";
            Currency currency = Currency.USD;
            BigDecimal initialDeposit = new BigDecimal("1000");

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Account result = accountService.createAccount(clientId, accountType, currency, initialDeposit);

            // Assert
            assertThat(result.getDailyWithdrawalLimit()).isEqualByComparingTo(expectedLimit);
            assertThat(result.getType()).isEqualTo(accountType);
        }

        private static Stream<Arguments> provideAccountTypesAndLimits() {
            return Stream.of(
                    Arguments.of(AccountType.SAVINGS, new BigDecimal("5000")),
                    Arguments.of(AccountType.CHECKING, new BigDecimal("10000")),
                    Arguments.of(AccountType.BUSINESS, new BigDecimal("50000"))
            );
        }

        /**
         * Demonstrates: Parameterized testing for different currencies
         * FIRST principles: Independent (isolated test for each currency), Self-validating
         * <p>
         * Tests account creation with different supported currencies.
         * Verifies that the system correctly handles USD, EUR, and UAH currencies.
         * This ensures multi-currency support works for account creation.
         */
        @ParameterizedTest
        @EnumSource(Currency.class)
        void createAccount_WithDifferentCurrencies_CreatesAccountSuccessfully(Currency currency) {
            // Arrange
            String clientId = "client-001";
            AccountType type = AccountType.CHECKING;
            BigDecimal initialDeposit = new BigDecimal("500");

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Account result = accountService.createAccount(clientId, type, currency, initialDeposit);

            // Assert
            assertThat(result.getCurrency()).isEqualTo(currency);
            assertThat(result.getBalance()).isEqualByComparingTo(initialDeposit);
        }

        /**
         * Demonstrates: Null parameter validation testing
         * FIRST principles: Fast (immediate validation), Self-validating (clear exception)
         * <p>
         * Tests that null clientId is properly rejected with IllegalArgumentException.
         * Input validation is critical - the service must reject invalid inputs before
         * any business logic executes or data is persisted.
         */
        @Test
        void createAccount_WithNullClientId_ThrowsException() {
            // Arrange
            String clientId = null;
            AccountType type = AccountType.CHECKING;
            Currency currency = Currency.USD;
            BigDecimal initialDeposit = new BigDecimal("1000");

            // Act & Assert
            assertThatThrownBy(() -> accountService.createAccount(clientId, type, currency, initialDeposit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Client ID, account type, and currency are required");

            verify(accountRepository, never()).save(any(Account.class));
        }

        /**
         * Demonstrates: Validation of null account type parameter
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that null account type is rejected before account creation.
         * This is part of defensive programming - validate all inputs early.
         */
        @Test
        void createAccount_WithNullAccountType_ThrowsException() {
            // Arrange
            String clientId = "client-001";
            AccountType type = null;
            Currency currency = Currency.USD;
            BigDecimal initialDeposit = new BigDecimal("1000");

            // Act & Assert
            assertThatThrownBy(() -> accountService.createAccount(clientId, type, currency, initialDeposit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Client ID, account type, and currency are required");

            verify(accountRepository, never()).save(any(Account.class));
        }

        /**
         * Demonstrates: Validation of null currency parameter
         * FIRST principles: Self-validating (clear failure reason)
         * <p>
         * Tests that null currency is rejected during account creation.
         * Currency is required for all accounts - without it, balance has no meaning.
         */
        @Test
        void createAccount_WithNullCurrency_ThrowsException() {
            // Arrange
            String clientId = "client-001";
            AccountType type = AccountType.CHECKING;
            Currency currency = null;
            BigDecimal initialDeposit = new BigDecimal("1000");

            // Act & Assert
            assertThatThrownBy(() -> accountService.createAccount(clientId, type, currency, initialDeposit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Client ID, account type, and currency are required");

            verify(accountRepository, never()).save(any(Account.class));
        }

        /**
         * Demonstrates: Edge case - negative initial deposit validation
         * FIRST principles: Fast (validation only), Independent
         * <p>
         * Tests that negative initial deposits are rejected.
         * Business rule: deposits must be non-negative. Negative amounts would
         * create accounts with debt, which violates account creation rules.
         */
        @Test
        void createAccount_WithNegativeInitialDeposit_ThrowsException() {
            // Arrange
            String clientId = "client-001";
            AccountType type = AccountType.CHECKING;
            Currency currency = Currency.USD;
            BigDecimal initialDeposit = new BigDecimal("-100");

            // Act & Assert
            assertThatThrownBy(() -> accountService.createAccount(clientId, type, currency, initialDeposit))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Initial deposit cannot be negative");

            verify(accountRepository, never()).save(any(Account.class));
        }

        /**
         * Demonstrates: Edge case - account creation with zero initial deposit
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that accounts can be created with zero balance.
         * This is a valid edge case - users should be able to open empty accounts.
         * Zero is non-negative, so it passes validation.
         */
        @Test
        void createAccount_WithZeroInitialDeposit_CreatesAccountSuccessfully() {
            // Arrange
            String clientId = "client-001";
            AccountType type = AccountType.CHECKING;
            Currency currency = Currency.USD;
            BigDecimal initialDeposit = BigDecimal.ZERO;

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Account result = accountService.createAccount(clientId, type, currency, initialDeposit);

            // Assert
            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(accountRepository).save(any(Account.class));
        }

        /**
         * Demonstrates: Edge case - account creation with null initial deposit
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that null initial deposit defaults to zero balance.
         * Business rule: if no initial deposit specified, create account with zero balance.
         * This is user-friendly - users don't have to explicitly pass zero.
         */
        @Test
        void createAccount_WithNullInitialDeposit_CreatesAccountWithZeroBalance() {
            // Arrange
            String clientId = "client-001";
            AccountType type = AccountType.CHECKING;
            Currency currency = Currency.USD;
            BigDecimal initialDeposit = null;

            when(timeProvider.now()).thenReturn(fixedTime);
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Account result = accountService.createAccount(clientId, type, currency, initialDeposit);

            // Assert
            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * Demonstrates: Edge case - withdrawal leaving exact threshold balance
         * FIRST principles: Fast (mocked dependencies), Independent
         * <p>
         * Tests boundary condition where withdrawal leaves exactly 100 (threshold).
         * Balance = threshold should NOT trigger low balance alert (only < threshold).
         * This tests the boundary: alert triggers at 99.99, but not at 100.00.
         */
        @Test
        void withdraw_LeavingExactThresholdBalance_DoesNotSendAlert() {
            // Arrange
            String accountId = "acc-001";
            BigDecimal initialBalance = new BigDecimal("200");
            BigDecimal withdrawAmount = new BigDecimal("100");

            Account account = createAccount(accountId, initialBalance);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(fraudDetectionService.isSuspicious(any(Account.class), any(BigDecimal.class))).thenReturn(false);

            // Act
            accountService.withdraw(accountId, withdrawAmount);

            // Assert - balance is exactly 100, should NOT trigger alert (threshold is < 100, not <= 100)
            verify(notificationService, never()).sendLowBalanceAlert(anyString(), anyString());
            verify(notificationService).sendTransactionNotification(eq(account.getClientId()), any());
        }

        /**
         * Demonstrates: Edge case - withdrawal leaving balance just below threshold
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests boundary condition where withdrawal leaves 99.99 (just below 100 threshold).
         * This should trigger low balance alert as balance < LOW_BALANCE_THRESHOLD (100).
         * Complements previous test - verifies alert triggers at 99.99 but not at 100.00.
         */
        @Test
        void withdraw_LeavingBalanceJustBelowThreshold_SendsAlert() {
            // Arrange
            String accountId = "acc-001";
            BigDecimal initialBalance = new BigDecimal("200");
            BigDecimal withdrawAmount = new BigDecimal("100.01");

            Account account = createAccount(accountId, initialBalance);
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(fraudDetectionService.isSuspicious(any(Account.class), any(BigDecimal.class))).thenReturn(false);

            // Act
            accountService.withdraw(accountId, withdrawAmount);

            // Assert - balance is 99.99, should trigger alert
            verify(notificationService).sendLowBalanceAlert(account.getClientId(), accountId);
            verify(notificationService).sendTransactionNotification(eq(account.getClientId()), any());
        }

        /**
         * Demonstrates: Account blocking scenario
         * FIRST principles: Fast, Independent (isolated state change test)
         * <p>
         * Tests that active accounts can be successfully blocked.
         * Blocking is an administrative action - freezes account operations
         * without closing the account permanently.
         */
        @Test
        void blockAccount_WithActiveAccount_BlocksSuccessfully() {
            // Arrange
            String accountId = "acc-001";
            Account account = createAccount(accountId, new BigDecimal("1000"));
            account.setStatus(AccountStatus.ACTIVE);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Account result = accountService.blockAccount(accountId);

            // Assert
            assertThat(result.getStatus()).isEqualTo(AccountStatus.BLOCKED);
            verify(accountRepository).save(account);
        }

        /**
         * Demonstrates: Account unblocking scenario with validation
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that blocked accounts can be unblocked back to active status.
         * This reverses the blocking operation - allows account to resume operations.
         */
        @Test
        void unblockAccount_WithBlockedAccount_UnblocksSuccessfully() {
            // Arrange
            String accountId = "acc-001";
            Account account = createAccount(accountId, new BigDecimal("1000"));
            account.setStatus(AccountStatus.BLOCKED);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Account result = accountService.unblockAccount(accountId);

            // Assert
            assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            verify(accountRepository).save(account);
        }

        /**
         * Demonstrates: Invalid operation - unblocking already active account
         * FIRST principles: Fast, Self-validating (clear business rule violation)
         * <p>
         * Tests that attempting to unblock an active account throws exception.
         * Business rule: can only unblock accounts that are currently blocked.
         * This prevents misuse of the unblock operation.
         */
        @Test
        void unblockAccount_WithActiveAccount_ThrowsException() {
            // Arrange
            String accountId = "acc-001";
            Account account = createAccount(accountId, new BigDecimal("1000"));
            account.setStatus(AccountStatus.ACTIVE);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // Act & Assert
            assertThatThrownBy(() -> accountService.unblockAccount(accountId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account is not blocked");

            verify(accountRepository, never()).save(any(Account.class));
        }

        /**
         * Demonstrates: Successful account closure with zero balance
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that accounts with zero balance can be closed.
         * Business rule: accounts must have zero balance before closure.
         * This is standard banking practice - settle all balances before closing.
         */
        @Test
        void closeAccount_WithZeroBalance_ClosesSuccessfully() {
            // Arrange
            String accountId = "acc-001";
            Account account = createAccount(accountId, BigDecimal.ZERO);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Account result = accountService.closeAccount(accountId);

            // Assert
            assertThat(result.getStatus()).isEqualTo(AccountStatus.CLOSED);
            verify(accountRepository).save(account);
        }

        /**
         * Demonstrates: Edge case - deposit to account not found
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that depositing to non-existent account throws exception.
         * Repository returns Optional.empty() for missing accounts.
         * Service must handle this gracefully with clear error message.
         */
        @Test
        void deposit_WithNonExistentAccount_ThrowsException() {
            // Arrange
            String accountId = "non-existent";
            BigDecimal amount = new BigDecimal("100");

            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.deposit(accountId, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account not found");

            verify(accountRepository, never()).save(any(Account.class));
        }

        /**
         * Demonstrates: Edge case - withdrawal from non-existent account
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that withdrawing from non-existent account throws exception.
         * Similar to deposit test, but for withdrawal operation.
         * Both must fail early with clear error when account doesn't exist.
         */
        @Test
        void withdraw_WithNonExistentAccount_ThrowsException() {
            // Arrange
            String accountId = "non-existent";
            BigDecimal amount = new BigDecimal("100");

            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountService.withdraw(accountId, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account not found");

            verify(accountRepository, never()).save(any(Account.class));
        }

        /**
         * Demonstrates: Suspicious withdrawal detection and blocking
         * FIRST principles: Fast, Independent (isolated fraud check)
         * <p>
         * Tests that suspicious withdrawals are detected and rejected.
         * Similar to deposit fraud test, but for withdrawal operations.
         * Both operations must be monitored for fraudulent activity.
         */
        @Test
        void withdraw_WithSuspiciousAmount_ThrowsFraudException() {
            // Arrange
            String accountId = "acc-001";
            BigDecimal withdrawAmount = new BigDecimal("50000");
            Account account = createAccount(accountId, new BigDecimal("60000"));

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(fraudDetectionService.isSuspicious(any(Account.class), any(BigDecimal.class))).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> accountService.withdraw(accountId, withdrawAmount))
                    .isInstanceOf(FraudDetectedException.class)
                    .hasMessageContaining("Suspicious withdrawal detected");

            verify(fraudDetectionService).isSuspicious(account, withdrawAmount);
            verify(accountRepository, never()).save(any(Account.class));
        }
    }
}