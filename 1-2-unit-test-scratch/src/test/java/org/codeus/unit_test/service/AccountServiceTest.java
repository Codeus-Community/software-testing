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

    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.of(2024, 1, 15, 10, 0);
    }

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

    @Test
    void createAccount_WithNullInitialDeposit_CreatesAccountWithZeroBalance() {
        // Arrange
        String clientId = "client-001";
        when(timeProvider.now()).thenReturn(fixedTime);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Account result = accountService.createAccount(clientId, AccountType.CHECKING, Currency.EUR, null);

        // Assert
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createAccount_WithNullClientId_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> accountService.createAccount(null, AccountType.SAVINGS, Currency.USD, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createAccount_WithNegativeInitialDeposit_ThrowsException() {
        // Arrange
        BigDecimal negativeAmount = new BigDecimal("-100");

        // Act & Assert
        assertThatThrownBy(() -> accountService.createAccount("client-001", AccountType.SAVINGS, Currency.USD, negativeAmount))
                .isInstanceOf(IllegalArgumentException.class);
    }

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
                .isInstanceOf(FraudDetectedException.class);

        verify(fraudDetectionService).isSuspicious(account, depositAmount);
        verify(accountRepository, never()).save(any(Account.class));
        verify(notificationService, never()).sendTransactionNotification(anyString(), any());
    }

    @Test
    void deposit_WithNonExistentAccount_ThrowsException() {
        // Arrange
        String accountId = "non-existent";
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.deposit(accountId, new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withdraw_WithSufficientFunds_DecreasesBalance() {
        // Arrange
        String accountId = "acc-001";
        BigDecimal initialBalance = new BigDecimal("1000");
        BigDecimal withdrawAmount = new BigDecimal("300");

        Account account = createAccount(accountId, initialBalance);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudDetectionService.isSuspicious(any(Account.class), any(BigDecimal.class))).thenReturn(false);

        // Act
        Account result = accountService.withdraw(accountId, withdrawAmount);

        // Assert
        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("700"));

        verify(transactionValidator).validateWithdrawal(account, withdrawAmount);
        verify(fraudDetectionService).isSuspicious(account, withdrawAmount);
        verify(accountRepository).save(account);
        verify(notificationService).sendTransactionNotification(eq(account.getClientId()), any());
    }

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
                .isInstanceOf(FraudDetectedException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

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

    @Test
    void unblockAccount_WithActiveAccount_ThrowsException() {
        // Arrange
        String accountId = "acc-001";
        Account account = createAccount(accountId, new BigDecimal("1000"));
        account.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> accountService.unblockAccount(accountId))
                .isInstanceOf(IllegalArgumentException.class);
    }

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

    @Test
    void closeAccount_WithPositiveBalance_ThrowsException() {
        // Arrange
        String accountId = "acc-001";
        Account account = createAccount(accountId, new BigDecimal("100"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act & Assert
        assertThatThrownBy(() -> accountService.closeAccount(accountId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccount_WithExistingAccount_ReturnsAccount() {
        // Arrange
        String accountId = "acc-001";
        Account account = createAccount(accountId, new BigDecimal("1000"));

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act
        Account result = accountService.getAccount(accountId);

        // Assert
        assertThat(result).isEqualTo(account);
        verify(accountRepository).findById(accountId);
    }

    @Test
    void getAccount_WithNonExistentAccount_ThrowsException() {
        // Arrange
        String accountId = "non-existent";
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> accountService.getAccount(accountId))
                .isInstanceOf(IllegalArgumentException.class);
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