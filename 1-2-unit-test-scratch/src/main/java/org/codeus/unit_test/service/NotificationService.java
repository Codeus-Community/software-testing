package org.codeus.unit_test.service;

import org.codeus.unit_test.model.Transaction;

public interface NotificationService {
    void sendTransactionNotification(String clientId, Transaction transaction);
    void sendLowBalanceAlert(String clientId, String accountId);
    void sendDailyLimitWarning(String clientId, String accountId);
}