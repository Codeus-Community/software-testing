package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.TransactionType;
import org.codeus.unit_test.model.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleNotificationService implements NotificationService {

    private final Map<String, List<String>> notificationLog = new HashMap<>();

    @Override
    public void sendTransactionNotification(String clientId, Transaction transaction) {
        if (clientId == null) {
            return;
        }

        String message = buildTransactionMessage(transaction);
        logNotification(clientId, message);
        System.out.println("NOTIFICATION to " + clientId + ": " + message);
    }

    @Override
    public void sendLowBalanceAlert(String clientId, String accountId) {
        if (clientId == null || accountId == null) {
            return;
        }

        String message = "LOW BALANCE ALERT: Your account " + accountId + " has fallen below the threshold";
        logNotification(clientId, message);
        System.out.println("ALERT to " + clientId + ": " + message);
    }

    @Override
    public void sendDailyLimitWarning(String clientId, String accountId) {
        if (clientId == null || accountId == null) {
            return;
        }

        String message = "DAILY LIMIT WARNING: You are approaching your daily withdrawal limit for account " + accountId;
        logNotification(clientId, message);
        System.out.println("WARNING to " + clientId + ": " + message);
    }

    public List<String> getNotificationsForClient(String clientId) {
        return notificationLog.getOrDefault(clientId, new ArrayList<>());
    }

    public void clearNotifications() {
        notificationLog.clear();
    }

    private void logNotification(String clientId, String message) {
        notificationLog.computeIfAbsent(clientId, k -> new ArrayList<>()).add(message);
    }

    private String buildTransactionMessage(Transaction transaction) {
        if (transaction == null) {
            return "Transaction completed successfully";
        }

        return switch (transaction.getType()) {
            case DEPOSIT -> "Deposit of " + transaction.getAmount() + " " + transaction.getCurrency() + " received";
            case WITHDRAWAL -> "Withdrawal of " + transaction.getAmount() + " " + transaction.getCurrency() + " processed";
            case TRANSFER -> "Transfer of " + transaction.getAmount() + " " + transaction.getCurrency() + " completed";
        };
    }
}