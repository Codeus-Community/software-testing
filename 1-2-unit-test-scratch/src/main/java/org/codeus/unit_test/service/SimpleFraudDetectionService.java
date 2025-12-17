package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.model.Transaction;

import java.math.BigDecimal;

public class SimpleFraudDetectionService implements FraudDetectionService {

    private static final BigDecimal SUSPICIOUS_AMOUNT_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal BUSINESS_SUSPICIOUS_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal BALANCE_MULTIPLIER_THRESHOLD = new BigDecimal("5");

    @Override
    public boolean isSuspicious(Account account, BigDecimal amount) {
        if (account == null || amount == null) {
            return false;
        }

        BigDecimal threshold = account.getType() == AccountType.BUSINESS
                ? BUSINESS_SUSPICIOUS_THRESHOLD
                : SUSPICIOUS_AMOUNT_THRESHOLD;

        if (amount.compareTo(threshold) > 0) {
            return true;
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = amount.divide(account.getBalance(), 2, BigDecimal.ROUND_HALF_UP);
            if (ratio.compareTo(BALANCE_MULTIPLIER_THRESHOLD) > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isSuspiciousTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
        if (fromAccount == null || toAccount == null || amount == null) {
            return false;
        }

        if (isSuspicious(fromAccount, amount)) {
            return true;
        }

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            BigDecimal currencyChangeThreshold = new BigDecimal("5000");
            if (amount.compareTo(currencyChangeThreshold) > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void reportSuspiciousActivity(Transaction transaction) {
        System.out.println("FRAUD ALERT: Suspicious transaction detected - " + transaction.getId());
    }
}