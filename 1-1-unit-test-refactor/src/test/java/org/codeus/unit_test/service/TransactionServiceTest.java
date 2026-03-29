package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.enums.TransactionType;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.model.Transaction;
import org.codeus.unit_test.repository.AccountRepository;
import org.codeus.unit_test.repository.TransactionRepository;
import org.codeus.unit_test.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionServiceTest {

    private StubAccountRepository accountRepository;
    private StubTransactionRepository transactionRepository;
    private RecordingTransactionValidator transactionValidator;
    private RecordingFraudDetectionService fraudDetectionService;
    private RecordingNotificationService notificationService;
    private StubExchangeRateService exchangeRateService;
    private StubTimeProvider timeProvider;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        accountRepository = new StubAccountRepository();
        transactionRepository = new StubTransactionRepository();
        transactionValidator = new RecordingTransactionValidator();
        fraudDetectionService = new RecordingFraudDetectionService();
        notificationService = new RecordingNotificationService();
        exchangeRateService = new StubExchangeRateService();
        timeProvider = new StubTimeProvider(LocalDateTime.of(2024, 3, 10, 9, 30));

        transactionService = new TransactionService(
                accountRepository,
                transactionRepository,
                transactionValidator,
                fraudDetectionService,
                notificationService,
                exchangeRateService,
                timeProvider
        );
    }

    /**
     * Refactor of the order-dependent and collaborator-coupled bad test (tests 1 and 2):
     * the test uses focused doubles for repositories and time so it validates TransactionService behavior only.
     */
    @Test
    void transfer_updatesBalancesWithoutDependingOnOtherComponentImplementations() {
        Account fromAccount = saveAccount("from-1", "client-1", Currency.USD, "5000.00", "2000.00");
        Account toAccount = saveAccount("to-1", "client-2", Currency.USD, "1500.00", "1000.00", AccountType.SAVINGS);

        Transaction transaction = transactionService.transfer(fromAccount.getId(), toAccount.getId(), new BigDecimal("300.00"));

        assertThat(transaction.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(accountRepository.findById(fromAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("4697.00"));
        assertThat(accountRepository.findById(toAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(transactionValidator.validatedTransfers).containsExactly("from-1->to-1:300.00");
        assertThat(notificationService.transactionNotifications).containsExactly("client-1", "client-2");
    }

    /**
     * Refactor of the slow online-service bad test (test 3):
     * use a local stubbed exchange rate service so the test stays fast, deterministic, and isolated.
     */
    @Test
    void transfer_betweenCurrencies_usesFastStubbedExchangeRates() {
        exchangeRateService.setRate(Currency.USD, Currency.EUR, new BigDecimal("0.92"));
        Account fromAccount = saveAccount("usd-account", "client-usd", Currency.USD, "3000.00", "5000.00");
        Account toAccount = saveAccount("eur-account", "client-eur", Currency.EUR, "200.00", "5000.00", AccountType.SAVINGS);

        Transaction transaction = transactionService.transfer(fromAccount.getId(), toAccount.getId(), new BigDecimal("100.00"));

        assertThat(transaction.getToAccountId()).isEqualTo("eur-account");
        assertThat(accountRepository.findById(fromAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("2899.00"));
        assertThat(accountRepository.findById(toAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("292.00"));
        assertThat(exchangeRateService.convertCalls).isEqualTo(1);
    }

    /**
     * Refactor of Eager test (test 4, part 1 deposit):
     * split the large scenario into a small deposit-focused test with one reason to fail.
     */
    @Test
    void deposit_updatesBalanceAndRecordsDeposit() {
        Account account = saveAccount("deposit-account", "client-10", Currency.USD, "4000.00", "3000.00");

        Transaction deposit = transactionService.deposit(account.getId(), new BigDecimal("200.00"), "Salary top up");

        assertThat(deposit.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(deposit.getToAccountId()).isEqualTo(account.getId());
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("4200.00"));
        assertThat(transactionRepository.findByAccountId(account.getId())).hasSize(1);
    }

    /**
     * Refactor of Eager test (test 4, part 1 transfer):
     * keep transfer behavior in its own focused test instead of combining deposit, withdrawal, history, and notifications.
     */
    @Test
    void transfer_updatesBothAccountsAndRecordsTransfer() {
        Account fromAccount = saveAccount("focused-from", "client-11", Currency.USD, "4200.00", "3000.00");
        Account toAccount = saveAccount("focused-to", "client-12", Currency.USD, "1000.00", "3000.00", AccountType.SAVINGS);

        Transaction transfer = transactionService.transfer(fromAccount.getId(), toAccount.getId(), new BigDecimal("300.00"));

        assertThat(transfer.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(accountRepository.findById(fromAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("3897.00"));
        assertThat(accountRepository.findById(toAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("1300.00"));
        assertThat(transactionRepository.findByAccountId(fromAccount.getId())).hasSize(1);
    }

    /**
     * Refactor of Eager test (test 4, part 1 withdraw):
     * check withdrawal behavior separately so the assertion failure points to a single concern.
     */
    @Test
    void withdraw_updatesBalanceAndSendsNotification() {
        Account account = saveAccount("withdraw-account", "client-13", Currency.USD, "1300.00", "3000.00", AccountType.SAVINGS);

        Transaction withdrawal = transactionService.withdraw(account.getId(), new BigDecimal("50.00"), "ATM cash");

        assertThat(withdrawal.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(notificationService.transactionNotifications).containsExactly("client-13");
    }

    /**
     * Refactor ofWeak assertions (test 5):
     * verify the returned withdrawal details and the persisted account balance instead of only checking that a value exists.
     */
    @Test
    void transfer_processedWithdrawnAndProvidesTransactionDetails() {
        Account account = saveAccount("weak-withdraw", "client-16", Currency.USD, "1500.00", "5000.00");

        Transaction transaction = transactionService.withdraw(account.getId(), new BigDecimal("100.00"), "ATM withdrawal");

        assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(transaction.getFromAccountId()).isEqualTo(account.getId());
        assertThat(transaction.getToAccountId()).isNull();
        assertThat(transaction.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(transaction.getDescription()).isEqualTo("ATM withdrawal");
        assertThat(transaction.getTimestamp()).isEqualTo(LocalDateTime.of(2024, 3, 10, 9, 30));
        assertThat(accountRepository.findById(account.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("1400.00"));
    }

    /**
     * Refactor of the reflection-based bad test (test 6):
     * verify fee behavior through the public API so the test describes observable business behavior, not internals.
     */
    @Test
    void transfer_appliesMaximumFeeThroughPublicBehavior() {
        Account fromAccount = saveAccount("from-max-fee", "client-20", Currency.USD, "10000.00", "9000.00");
        Account toAccount = saveAccount("to-max-fee", "client-21", Currency.USD, "500.00", "1000.00", AccountType.SAVINGS);

        Transaction transaction = transactionService.transfer(fromAccount.getId(), toAccount.getId(), new BigDecimal("6000.00"));

        assertThat(transaction.getFee()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(accountRepository.findById(fromAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("3950.00"));
        assertThat(accountRepository.findById(toAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(new BigDecimal("6500.00"));
        assertThat(fraudDetectionService.suspiciousTransferChecks).containsExactly("from-max-fee->to-max-fee:6000.00");
    }

    private Account saveAccount(String id, String clientId, Currency currency, String balance, String dailyLimit) {
        return saveAccount(id, clientId, currency, balance, dailyLimit, AccountType.CHECKING);
    }

    private Account saveAccount(String id, String clientId, Currency currency, String balance, String dailyLimit, AccountType type) {
        Account account = Account.builder()
                .id(id)
                .clientId(clientId)
                .type(type)
                .currency(currency)
                .balance(new BigDecimal(balance))
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal(dailyLimit))
                .build();
        accountRepository.save(account);
        return account;
    }

    private static class StubAccountRepository implements AccountRepository {
        private final Map<String, Account> accounts = new HashMap<>();

        @Override
        public Account save(Account account) {
            accounts.put(account.getId(), account);
            return account;
        }

        @Override
        public Optional<Account> findById(String id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public List<Account> findByClientId(String clientId) {
            return accounts.values().stream()
                    .filter(account -> clientId.equals(account.getClientId()))
                    .collect(Collectors.toList());
        }

        @Override
        public void delete(String id) {
            accounts.remove(id);
        }

        @Override
        public List<Account> findAll() {
            return new ArrayList<>(accounts.values());
        }
    }

    private static class StubTransactionRepository implements TransactionRepository {
        private final List<Transaction> savedTransactions = new ArrayList<>();
        private final List<Transaction> transactionsForToday = new ArrayList<>();

        @Override
        public Transaction save(Transaction transaction) {
            savedTransactions.add(transaction);
            transactionsForToday.add(transaction);
            return transaction;
        }

        @Override
        public Optional<Transaction> findById(String id) {
            return savedTransactions.stream().filter(transaction -> id.equals(transaction.getId())).findFirst();
        }

        @Override
        public List<Transaction> findByAccountId(String accountId) {
            return savedTransactions.stream()
                    .filter(transaction -> accountId.equals(transaction.getFromAccountId()) || accountId.equals(transaction.getToAccountId()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Transaction> findByAccountIdAndDateRange(String accountId, LocalDateTime from, LocalDateTime to) {
            return transactionsForToday.stream()
                    .filter(transaction -> accountId.equals(transaction.getFromAccountId()) || accountId.equals(transaction.getToAccountId()))
                    .filter(transaction -> !transaction.getTimestamp().isBefore(from) && !transaction.getTimestamp().isAfter(to))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Transaction> findAll() {
            return new ArrayList<>(savedTransactions);
        }
    }

    private static class RecordingTransactionValidator extends TransactionValidator {
        private final List<String> validatedTransfers = new ArrayList<>();

        @Override
        public void validateTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
            validatedTransfers.add(fromAccount.getId() + "->" + toAccount.getId() + ":" + amount.setScale(2, RoundingMode.UNNECESSARY));
        }

        @Override
        public void validateWithdrawal(Account account, BigDecimal amount) {
        }

        @Override
        public void validateDailyLimit(Account account, BigDecimal amount, BigDecimal alreadyWithdrawnToday) {
        }

        @Override
        public void validateDeposit(Account account, BigDecimal amount) {
        }
    }

    private static class RecordingFraudDetectionService implements FraudDetectionService {
        private final List<String> suspiciousTransferChecks = new ArrayList<>();

        @Override
        public boolean isSuspicious(Account account, BigDecimal amount) {
            return false;
        }

        @Override
        public boolean isSuspiciousTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
            suspiciousTransferChecks.add(fromAccount.getId() + "->" + toAccount.getId() + ":" + amount.setScale(2, RoundingMode.UNNECESSARY));
            return false;
        }

        @Override
        public void reportSuspiciousActivity(Transaction transaction) {
        }
    }

    private static class RecordingNotificationService implements NotificationService {
        private final List<String> transactionNotifications = new ArrayList<>();

        @Override
        public void sendTransactionNotification(String clientId, Transaction transaction) {
            transactionNotifications.add(clientId);
        }

        @Override
        public void sendLowBalanceAlert(String clientId, String accountId) {
        }

        @Override
        public void sendDailyLimitWarning(String clientId, String accountId) {
        }
    }

    private static class StubExchangeRateService implements ExchangeRateService {
        private final Map<String, BigDecimal> rates = new HashMap<>();
        private int convertCalls;

        @Override
        public BigDecimal getExchangeRate(Currency from, Currency to) {
            if (from == to) {
                return BigDecimal.ONE;
            }

            BigDecimal directRate = rates.get(from + "_" + to);
            if (directRate != null) {
                return directRate;
            }

            BigDecimal reverseRate = rates.get(to + "_" + from);
            if (reverseRate != null) {
                return BigDecimal.ONE.divide(reverseRate, 6, RoundingMode.HALF_UP);
            }

            throw new IllegalArgumentException("Missing stubbed rate for " + from + " to " + to);
        }

        @Override
        public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
            convertCalls++;
            return amount.multiply(getExchangeRate(from, to)).setScale(2, RoundingMode.HALF_UP);
        }

        private void setRate(Currency from, Currency to, BigDecimal rate) {
            rates.put(from + "_" + to, rate);
        }
    }

    private static class StubTimeProvider implements TimeProvider {
        private final LocalDateTime now;

        private StubTimeProvider(LocalDateTime now) {
            this.now = now;
        }

        @Override
        public LocalDateTime now() {
            return now;
        }
    }
}
