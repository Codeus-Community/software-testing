package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.enums.TransactionType;
import org.codeus.unit_test.exception.FraudDetectedException;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.model.Transaction;
import org.codeus.unit_test.repository.AccountRepository;
import org.codeus.unit_test.repository.TransactionRepository;
import org.codeus.unit_test.util.TimeProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionValidator transactionValidator;
    private final FraudDetectionService fraudDetectionService;
    private final NotificationService notificationService;
    private final ExchangeRateService exchangeRateService;
    private final TimeProvider timeProvider;

    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal MIN_FEE = new BigDecimal("1");
    private static final BigDecimal MAX_FEE = new BigDecimal("50");

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              TransactionValidator transactionValidator,
                              FraudDetectionService fraudDetectionService,
                              NotificationService notificationService,
                              ExchangeRateService exchangeRateService,
                              TimeProvider timeProvider) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionValidator = transactionValidator;
        this.fraudDetectionService = fraudDetectionService;
        this.notificationService = notificationService;
        this.exchangeRateService = exchangeRateService;
        this.timeProvider = timeProvider;
    }

    public Transaction transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + fromAccountId));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + toAccountId));

        transactionValidator.validateTransfer(fromAccount, toAccount, amount);

        if (fraudDetectionService.isSuspiciousTransfer(fromAccount, toAccount, amount)) {
            Transaction suspiciousTransaction = buildTransaction(fromAccountId, toAccountId, amount,
                    fromAccount.getCurrency(), TransactionType.TRANSFER, "BLOCKED - Suspicious activity");
            fraudDetectionService.reportSuspiciousActivity(suspiciousTransaction);
            throw new FraudDetectedException("Suspicious transfer detected");
        }

        BigDecimal fee = calculateTransferFee(amount);
        BigDecimal totalDeduction = amount.add(fee);

        BigDecimal amountInTargetCurrency = amount;
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            amountInTargetCurrency = exchangeRateService.convert(
                    amount,
                    fromAccount.getCurrency(),
                    toAccount.getCurrency()
            );
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDeduction));
        toAccount.setBalance(toAccount.getBalance().add(amountInTargetCurrency));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = buildTransaction(fromAccountId, toAccountId, amount,
                fromAccount.getCurrency(), TransactionType.TRANSFER,
                "Transfer from " + fromAccountId + " to " + toAccountId);
        transaction.setFee(fee);

        Transaction savedTransaction = transactionRepository.save(transaction);

        notificationService.sendTransactionNotification(fromAccount.getClientId(), savedTransaction);
        notificationService.sendTransactionNotification(toAccount.getClientId(), savedTransaction);

        return savedTransaction;
    }

    public Transaction deposit(String accountId, BigDecimal amount, String description) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        transactionValidator.validateDeposit(account, amount);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = buildTransaction(null, accountId, amount,
                account.getCurrency(), TransactionType.DEPOSIT, description);

        Transaction savedTransaction = transactionRepository.save(transaction);
        notificationService.sendTransactionNotification(account.getClientId(), savedTransaction);

        return savedTransaction;
    }

    public Transaction withdraw(String accountId, BigDecimal amount, String description) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        transactionValidator.validateWithdrawal(account, amount);

        BigDecimal totalWithdrawnToday = getTotalWithdrawnToday(accountId);
        transactionValidator.validateDailyLimit(account, amount, totalWithdrawnToday);

        if (totalWithdrawnToday.add(amount).compareTo(account.getDailyWithdrawalLimit().multiply(new BigDecimal("0.9"))) > 0) {
            notificationService.sendDailyLimitWarning(account.getClientId(), accountId);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = buildTransaction(accountId, null, amount,
                account.getCurrency(), TransactionType.WITHDRAWAL, description);

        Transaction savedTransaction = transactionRepository.save(transaction);
        notificationService.sendTransactionNotification(account.getClientId(), savedTransaction);

        return savedTransaction;
    }

    public List<Transaction> getAccountTransactions(String accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> getAccountTransactionsByDateRange(String accountId, LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findByAccountIdAndDateRange(accountId, from, to);
    }

    private BigDecimal calculateTransferFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(TRANSFER_FEE_RATE);

        if (fee.compareTo(MIN_FEE) < 0) {
            return MIN_FEE;
        }

        if (fee.compareTo(MAX_FEE) > 0) {
            return MAX_FEE;
        }

        return fee.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal getTotalWithdrawnToday(String accountId) {
        LocalDateTime startOfDay = timeProvider.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<Transaction> todayTransactions = transactionRepository
                .findByAccountIdAndDateRange(accountId, startOfDay, endOfDay);

        return todayTransactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .filter(t -> accountId.equals(t.getFromAccountId()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Transaction buildTransaction(String fromAccountId, String toAccountId, BigDecimal amount,
                                         Currency currency, TransactionType type, String description) {
        return Transaction.builder()
                .id(UUID.randomUUID().toString())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .type(type)
                .amount(amount)
                .currency(currency)
                .timestamp(timeProvider.now())
                .description(description)
                .fee(BigDecimal.ZERO)
                .build();
    }
}