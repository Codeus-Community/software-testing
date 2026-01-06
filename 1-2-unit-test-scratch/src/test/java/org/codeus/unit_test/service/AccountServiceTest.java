package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.exception.FraudDetectedException;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.repository.AccountRepository;
import org.codeus.unit_test.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
}