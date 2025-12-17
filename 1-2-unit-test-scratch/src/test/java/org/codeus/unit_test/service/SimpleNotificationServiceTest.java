package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.enums.TransactionType;
import org.codeus.unit_test.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleNotificationServiceTest {

    private SimpleNotificationService notificationService;
    private String clientId;

    @BeforeEach
    void setUp() {
        notificationService = new SimpleNotificationService();
        clientId = "client-001";
    }

    @Test
    void sendTransactionNotification_WithDepositTransaction_LogsNotification() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id("txn-001")
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500"))
                .currency(Currency.USD)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        notificationService.sendTransactionNotification(clientId, transaction);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0)).contains("Deposit");
        assertThat(notifications.get(0)).contains("500");
        assertThat(notifications.get(0)).contains("USD");
    }

    @Test
    void sendTransactionNotification_WithWithdrawalTransaction_LogsNotification() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id("txn-002")
                .type(TransactionType.WITHDRAWAL)
                .amount(new BigDecimal("200"))
                .currency(Currency.EUR)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        notificationService.sendTransactionNotification(clientId, transaction);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0)).contains("Withdrawal");
        assertThat(notifications.get(0)).contains("200");
        assertThat(notifications.get(0)).contains("EUR");
    }

    @Test
    void sendTransactionNotification_WithTransferTransaction_LogsNotification() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id("txn-003")
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("1000"))
                .currency(Currency.UAH)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        notificationService.sendTransactionNotification(clientId, transaction);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0)).contains("Transfer");
        assertThat(notifications.get(0)).contains("1000");
        assertThat(notifications.get(0)).contains("UAH");
    }

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void sendTransactionNotification_ForAllTransactionTypes_LogsNotification(TransactionType type) {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id("txn-" + type)
                .type(type)
                .amount(new BigDecimal("100"))
                .currency(Currency.USD)
                .timestamp(LocalDateTime.now())
                .build();

        // Act
        notificationService.sendTransactionNotification(clientId, transaction);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0)).isNotEmpty();
    }

    @Test
    void sendTransactionNotification_WithNullTransaction_LogsDefaultMessage() {
        // Act
        notificationService.sendTransactionNotification(clientId, null);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0)).contains("Transaction completed successfully");
    }

    @Test
    void sendTransactionNotification_WithNullClientId_DoesNotLog() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id("txn-001")
                .type(TransactionType.DEPOSIT)
                .amount(new BigDecimal("500"))
                .currency(Currency.USD)
                .build();

        // Act
        notificationService.sendTransactionNotification(null, transaction);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).isEmpty();
    }

    @Test
    void sendLowBalanceAlert_LogsAlert() {
        // Arrange
        String accountId = "acc-001";

        // Act
        notificationService.sendLowBalanceAlert(clientId, accountId);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0)).contains("LOW BALANCE ALERT");
        assertThat(notifications.get(0)).contains(accountId);
    }

    @Test
    void sendLowBalanceAlert_WithNullClientId_DoesNotLog() {
        // Arrange
        String accountId = "acc-001";

        // Act
        notificationService.sendLowBalanceAlert(null, accountId);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).isEmpty();
    }

    @Test
    void sendLowBalanceAlert_WithNullAccountId_DoesNotLog() {
        // Act
        notificationService.sendLowBalanceAlert(clientId, null);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).isEmpty();
    }

    @Test
    void sendDailyLimitWarning_LogsWarning() {
        // Arrange
        String accountId = "acc-002";

        // Act
        notificationService.sendDailyLimitWarning(clientId, accountId);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0)).contains("DAILY LIMIT WARNING");
        assertThat(notifications.get(0)).contains(accountId);
    }

    @Test
    void sendDailyLimitWarning_WithNullClientId_DoesNotLog() {
        // Arrange
        String accountId = "acc-002";

        // Act
        notificationService.sendDailyLimitWarning(null, accountId);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).isEmpty();
    }

    @Test
    void sendDailyLimitWarning_WithNullAccountId_DoesNotLog() {
        // Act
        notificationService.sendDailyLimitWarning(clientId, null);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).isEmpty();
    }

    @Test
    void getNotificationsForClient_WithNoNotifications_ReturnsEmptyList() {
        // Act
        List<String> notifications = notificationService.getNotificationsForClient(clientId);

        // Assert
        assertThat(notifications).isEmpty();
    }

    @Test
    void getNotificationsForClient_WithMultipleNotifications_ReturnsAllNotifications() {
        // Arrange
        Transaction transaction1 = createTransaction(TransactionType.DEPOSIT, "100");
        Transaction transaction2 = createTransaction(TransactionType.WITHDRAWAL, "50");

        // Act
        notificationService.sendTransactionNotification(clientId, transaction1);
        notificationService.sendTransactionNotification(clientId, transaction2);
        notificationService.sendLowBalanceAlert(clientId, "acc-001");

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(3);
    }

    @Test
    void getNotificationsForClient_ForDifferentClients_ReturnsSeparateNotifications() {
        // Arrange
        String client1 = "client-001";
        String client2 = "client-002";
        Transaction transaction = createTransaction(TransactionType.DEPOSIT, "100");

        // Act
        notificationService.sendTransactionNotification(client1, transaction);
        notificationService.sendLowBalanceAlert(client2, "acc-002");

        // Assert
        List<String> notifications1 = notificationService.getNotificationsForClient(client1);
        List<String> notifications2 = notificationService.getNotificationsForClient(client2);

        assertThat(notifications1).hasSize(1);
        assertThat(notifications2).hasSize(1);
        assertThat(notifications1.get(0)).contains("Deposit");
        assertThat(notifications2.get(0)).contains("LOW BALANCE");
    }

    @Test
    void clearNotifications_RemovesAllNotifications() {
        // Arrange
        Transaction transaction = createTransaction(TransactionType.DEPOSIT, "100");
        notificationService.sendTransactionNotification(clientId, transaction);
        notificationService.sendLowBalanceAlert(clientId, "acc-001");

        // Act
        notificationService.clearNotifications();

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).isEmpty();
    }

    @Test
    void clearNotifications_AfterClear_CanAddNewNotifications() {
        // Arrange
        Transaction transaction1 = createTransaction(TransactionType.DEPOSIT, "100");
        notificationService.sendTransactionNotification(clientId, transaction1);
        notificationService.clearNotifications();

        // Act
        Transaction transaction2 = createTransaction(TransactionType.WITHDRAWAL, "50");
        notificationService.sendTransactionNotification(clientId, transaction2);

        // Assert
        List<String> notifications = notificationService.getNotificationsForClient(clientId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0)).contains("Withdrawal");
    }

    private Transaction createTransaction(TransactionType type, String amount) {
        return Transaction.builder()
                .id("txn-" + System.nanoTime())
                .type(type)
                .amount(new BigDecimal(amount))
                .currency(Currency.USD)
                .timestamp(LocalDateTime.now())
                .build();
    }
}