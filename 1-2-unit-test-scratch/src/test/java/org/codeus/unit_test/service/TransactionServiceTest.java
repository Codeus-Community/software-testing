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
    private ExchangeRateService exchangeRateService;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;
    private LocalDateTime fixedTime;

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
        assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("3990"));
        assertThat(toAccount.getBalance()).isEqualByComparingTo(new BigDecimal("3000"));

        verify(transactionValidator).validateTransfer(fromAccount, toAccount, amount);
        verify(fraudDetectionService).isSuspiciousTransfer(fromAccount, toAccount, amount);
        verify(accountRepository).save(fromAccount);
        verify(accountRepository).save(toAccount);
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationService, times(2)).sendTransactionNotification(anyString(), any(Transaction.class));
    }

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
        InOrder inOrder = inOrder(transactionValidator, fraudDetectionService, accountRepository, transactionRepository, notificationService);
        inOrder.verify(transactionValidator).validateTransfer(fromAccount, toAccount, amount);
        inOrder.verify(fraudDetectionService).isSuspiciousTransfer(fromAccount, toAccount, amount);
        inOrder.verify(accountRepository).save(fromAccount);
        inOrder.verify(accountRepository).save(toAccount);
        inOrder.verify(transactionRepository).save(any(Transaction.class));
        inOrder.verify(notificationService).sendTransactionNotification(eq("client-001"), any(Transaction.class));
        inOrder.verify(notificationService).sendTransactionNotification(eq("client-002"), any(Transaction.class));
    }

    @Test
    void transfer_WithDifferentCurrencies_ConvertsAmount() {
        // Arrange
        toAccount.setCurrency(Currency.EUR);
        BigDecimal amount = new BigDecimal("1000");
        BigDecimal convertedAmount = new BigDecimal("920");

        when(timeProvider.now()).thenReturn(fixedTime);
        when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-002")).thenReturn(Optional.of(toAccount));
        when(fraudDetectionService.isSuspiciousTransfer(any(), any(), any())).thenReturn(false);
        when(exchangeRateService.convert(amount, Currency.USD, Currency.EUR)).thenReturn(convertedAmount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        transactionService.transfer("acc-001", "acc-002", amount);

        // Assert
        verify(exchangeRateService).convert(amount, Currency.USD, Currency.EUR);
        assertThat(toAccount.getBalance()).isEqualByComparingTo(new BigDecimal("2920"));
    }

    @Test
    void transfer_WithSuspiciousActivity_ThrowsExceptionAndDoesNotTransfer() {
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

    @Test
    void transfer_CalculatesFeeCorrectly() {
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
        assertThat(result.getFee()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void transfer_WithFromAccountNotFound_ThrowsException() {
        // Arrange
        when(accountRepository.findById("acc-001")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.transfer("acc-001", "acc-002", new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transfer_WithToAccountNotFound_ThrowsException() {
        // Arrange
        when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-002")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transactionService.transfer("acc-001", "acc-002", new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deposit_CreatesDepositTransaction() {
        // Arrange
        BigDecimal amount = new BigDecimal("500");
        String description = "Salary deposit";

        when(timeProvider.now()).thenReturn(fixedTime);
        when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Transaction result = transactionService.deposit("acc-001", amount, description);

        // Assert
        assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("5500"));

        verify(transactionValidator).validateDeposit(fromAccount, amount);
        verify(accountRepository).save(fromAccount);
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationService).sendTransactionNotification(eq("client-001"), any(Transaction.class));
    }

    @Test
    void withdraw_CreatesWithdrawalTransaction() {
        // Arrange
        BigDecimal amount = new BigDecimal("300");
        String description = "ATM withdrawal";

        when(timeProvider.now()).thenReturn(fixedTime);
        when(accountRepository.findById("acc-001")).thenReturn(Optional.of(fromAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByAccountIdAndDateRange(anyString(), any(), any())).thenReturn(List.of());

        // Act
        Transaction result = transactionService.withdraw("acc-001", amount, description);

        // Assert
        assertThat(result.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(fromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("4700"));

        verify(transactionValidator).validateWithdrawal(fromAccount, amount);
        verify(transactionValidator).validateDailyLimit(eq(fromAccount), eq(amount), any(BigDecimal.class));
        verify(accountRepository).save(fromAccount);
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationService).sendTransactionNotification(eq("client-001"), any(Transaction.class));
    }

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

        // Assert (7500 + 1600 = 9100 > 9000, triggers warning)
        verify(notificationService).sendDailyLimitWarning("client-001", "acc-001");
    }

    @Test
    void getAccountTransactions_ReturnsTransactionList() {
        // Arrange
        String accountId = "acc-001";
        List<Transaction> expectedTransactions = List.of(
                Transaction.builder().id("txn-001").build(),
                Transaction.builder().id("txn-002").build()
        );

        when(transactionRepository.findByAccountId(accountId)).thenReturn(expectedTransactions);

        // Act
        List<Transaction> result = transactionService.getAccountTransactions(accountId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedTransactions);
        verify(transactionRepository).findByAccountId(accountId);
    }

    @Test
    void getAccountTransactionsByDateRange_ReturnsFilteredTransactions() {
        // Arrange
        String accountId = "acc-001";
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2024, 1, 31, 23, 59);
        List<Transaction> expectedTransactions = List.of(
                Transaction.builder().id("txn-001").build()
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
}