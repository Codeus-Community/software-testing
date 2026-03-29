package org.codeus.unit_test.service;

import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.model.Transaction;

import java.math.BigDecimal;

public interface FraudDetectionService {
    boolean isSuspicious(Account account, BigDecimal amount);
    boolean isSuspiciousTransfer(Account fromAccount, Account toAccount, BigDecimal amount);
    void reportSuspiciousActivity(Transaction transaction);
}