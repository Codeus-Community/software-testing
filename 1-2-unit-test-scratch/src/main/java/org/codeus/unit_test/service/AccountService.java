package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.exception.FraudDetectedException;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.repository.AccountRepository;
import org.codeus.unit_test.util.TimeProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionValidator transactionValidator;
    private final NotificationService notificationService;
    private final FraudDetectionService fraudDetectionService;


    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("100");

    public AccountService(AccountRepository accountRepository,
                          TransactionValidator transactionValidator,
                          NotificationService notificationService,
                          FraudDetectionService fraudDetectionService) {
        this.accountRepository = accountRepository;
        this.transactionValidator = transactionValidator;
        this.notificationService = notificationService;
        this.fraudDetectionService = fraudDetectionService;
    }

    public Account createAccount(String clientId, AccountType type, Currency currency, BigDecimal initialDeposit) {
        if (clientId == null || type == null || currency == null) {
            throw new IllegalArgumentException("Client ID, account type, and currency are required");
        }

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative");
        }

        Account account = Account.builder()
                .id(UUID.randomUUID().toString())
                .clientId(clientId)
                .type(type)
                .currency(currency)
                .balance(initialDeposit != null ? initialDeposit : BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .dailyWithdrawalLimit(getDefaultDailyLimit(type))
                .build();

        return accountRepository.save(account);
    }

    public Account deposit(String accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        transactionValidator.validateDeposit(account, amount);

        if (fraudDetectionService.isSuspicious(account, amount)) {
            throw new FraudDetectedException("Suspicious deposit detected for account: " + accountId);
        }

        account.setBalance(account.getBalance().add(amount));
        Account savedAccount = accountRepository.save(account);

        notificationService.sendTransactionNotification(account.getClientId(), null);

        return savedAccount;
    }

    public Account withdraw(String accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        transactionValidator.validateWithdrawal(account, amount);

        if (fraudDetectionService.isSuspicious(account, amount)) {
            throw new FraudDetectedException("Suspicious withdrawal detected for account: " + accountId);
        }

        account.setBalance(account.getBalance().subtract(amount));
        Account savedAccount = accountRepository.save(account);

        if (savedAccount.getBalance().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
            notificationService.sendLowBalanceAlert(account.getClientId(), accountId);
        }

        notificationService.sendTransactionNotification(account.getClientId(), null);

        return savedAccount;
    }

    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

    public Account blockAccount(String accountId) {
        Account account = getAccount(accountId);
        account.setStatus(AccountStatus.BLOCKED);
        return accountRepository.save(account);
    }

    public Account unblockAccount(String accountId) {
        Account account = getAccount(accountId);

        if (account.getStatus() != AccountStatus.BLOCKED) {
            throw new IllegalArgumentException("Account is not blocked: " + accountId);
        }

        account.setStatus(AccountStatus.ACTIVE);
        return accountRepository.save(account);
    }

    public Account closeAccount(String accountId) {
        Account account = getAccount(accountId);

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Cannot close account with positive balance: " + accountId);
        }

        account.setStatus(AccountStatus.CLOSED);
        return accountRepository.save(account);
    }

    private BigDecimal getDefaultDailyLimit(AccountType type) {
        return switch (type) {
            case SAVINGS -> new BigDecimal("5000");
            case CHECKING -> new BigDecimal("10000");
            case BUSINESS -> new BigDecimal("50000");
        };
    }
}