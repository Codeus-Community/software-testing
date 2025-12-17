package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.exception.AccountBlockedException;
import org.codeus.unit_test.exception.InsufficientFundsException;
import org.codeus.unit_test.exception.InvalidTransactionException;
import org.codeus.unit_test.model.Account;

import java.math.BigDecimal;

public class TransactionValidator {

    public void validateWithdrawal(Account account, BigDecimal amount) {
        validateAccount(account);
        validateAmount(amount);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Balance: " + account.getBalance() + ", requested: " + amount
            );
        }
    }

    public void validateDeposit(Account account, BigDecimal amount) {
        validateAccount(account);
        validateAmount(amount);
    }

    public void validateTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
        validateAccount(fromAccount);
        validateAccount(toAccount);
        validateAmount(amount);

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds for transfer. Balance: " + fromAccount.getBalance() + ", requested: " + amount
            );
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }
    }

    public void validateDailyLimit(Account account, BigDecimal amount, BigDecimal alreadyWithdrawnToday) {
        validateAccount(account);
        validateAmount(amount);

        BigDecimal totalWithdrawal = alreadyWithdrawnToday.add(amount);

        if (account.getDailyWithdrawalLimit() != null &&
                totalWithdrawal.compareTo(account.getDailyWithdrawalLimit()) > 0) {
            throw new InvalidTransactionException(
                    "Daily withdrawal limit exceeded. Limit: " + account.getDailyWithdrawalLimit() +
                            ", already withdrawn: " + alreadyWithdrawnToday + ", requested: " + amount
            );
        }
    }

    private void validateAccount(Account account) {
        if (account == null) {
            throw new InvalidTransactionException("Account cannot be null");
        }

        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new AccountBlockedException("Account is blocked: " + account.getId());
        }

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidTransactionException("Account is closed: " + account.getId());
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionException("Amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount must be positive: " + amount);
        }
    }
}