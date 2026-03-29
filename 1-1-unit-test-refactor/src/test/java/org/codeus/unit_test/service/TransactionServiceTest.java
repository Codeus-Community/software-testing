package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.model.Transaction;
import org.codeus.unit_test.repository.InMemoryAccountRepository;
import org.codeus.unit_test.repository.InMemoryTransactionRepository;
import org.codeus.unit_test.util.SystemTimeProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

//TODO:
// - analyze all test cases below
// - refactor the test cases using information from the presentation (FIRST principles, best practices... and common sense)
// _____
// Note: the number of refactored test cases can differ from this original tests. E.g. some of tests may be merged or split.
@TestMethodOrder(OrderAnnotation.class)
class TransactionServiceTest {

    private static InMemoryAccountRepository accountRepository;
    private static InMemoryTransactionRepository transactionRepository;
    private static SimpleNotificationService notificationService;
    private static TransactionService transactionService;

    @BeforeAll
    static void setUpSuite() {
        accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        notificationService = new SimpleNotificationService();

        transactionService = new TransactionService(
                accountRepository,
                transactionRepository,
                new TransactionValidator(),
                new SimpleFraudDetectionService(notificationService),
                notificationService,
                new SimpleExchangeRateService(() -> Map.of(
                        "USD_EUR", new BigDecimal("0.92"),
                        "USD_UAH", new BigDecimal("41.50"),
                        "EUR_UAH", new BigDecimal("45.00")
                )),
                new SystemTimeProvider()
        );
    }

    @Test
    @Order(1)
    void transfer_successfullyTransfersBalanceBetweenCheckingAndSavingsAccounts() {
        accountRepository.save(account("shared-from", "client-1", AccountType.CHECKING, Currency.USD, "5000.00", "2000.00"));
        accountRepository.save(account("shared-to", "client-2", AccountType.SAVINGS, Currency.USD, "1500.00", "1000.00"));

        transactionService.transfer("shared-from", "shared-to", new BigDecimal("300.00"));

        System.out.println("Results after 'transfer' operation:");
        System.out.println("fromAccount balance = " + accountRepository.findById("shared-from").orElseThrow().getBalance());
        System.out.println("toAccount balance = " + accountRepository.findById("shared-to").orElseThrow().getBalance());
    }

    @Test
    @Order(2)
    void withdraw_successfullyWithdrawsBalanceFromCheckingAccount() {
        transactionService.withdraw("shared-from", new BigDecimal("400.00"), "Cash withdrawal after transfer");

        System.out.println("Results after 'withdraw' operation:");
        System.out.println("fromAccount balance = " + accountRepository.findById("shared-from").orElseThrow().getBalance());
        System.out.println("toAccount balance = " + accountRepository.findById("shared-to").orElseThrow().getBalance());
    }

    @Test
    @Order(3)
    void transfer_successfullyTransfersBalanceUsingOnlineExchangeRates() {
        TransactionService onlineTransactionService = new TransactionService(
                accountRepository,
                transactionRepository,
                new TransactionValidator(),
                new SimpleFraudDetectionService(notificationService), notificationService,
                new OnlineExchangeRateService(() -> Map.of(
                        "USD_EUR", new BigDecimal("0.92"),
                        "USD_UAH", new BigDecimal("41.50"),
                        "EUR_UAH", new BigDecimal("45.00")
                )),
                new SystemTimeProvider()
        );

        accountRepository.save(account("usd-account", "client-usd", AccountType.CHECKING, Currency.USD, "3000.00", "5000.00"));
        accountRepository.save(account("eur-account", "client-eur", AccountType.SAVINGS, Currency.EUR, "200.00", "5000.00"));

        long startedAt = System.currentTimeMillis();
        Transaction transaction = onlineTransactionService.transfer("usd-account", "eur-account", new BigDecimal("100.00"));
        long elapsedMillis = System.currentTimeMillis() - startedAt;

        System.out.println("Transfer completed in " + elapsedMillis + " ms using simulated online rates.");
        assertThat(transaction.getToAccountId()).isEqualTo("eur-account");
    }

    @Test
    @Order(4)
    void transfer_deposit_withdraw_history_and_notifications_successfullyFinished() {
        accountRepository.save(account("eager-from", "client-10", AccountType.CHECKING, Currency.USD, "4000.00", "3000.00"));
        accountRepository.save(account("eager-to", "client-11", AccountType.SAVINGS, Currency.USD, "1000.00", "3000.00"));

        Transaction deposit = transactionService.deposit("eager-from", new BigDecimal("200.00"), "Salary top up");
        Transaction transfer = transactionService.transfer("eager-from", "eager-to", new BigDecimal("300.00"));
        Transaction withdrawal = transactionService.withdraw("eager-to", new BigDecimal("50.00"), "ATM cash");
        List<Transaction> eagerFromTransactions = transactionService.getAccountTransactions("eager-from");
        List<Transaction> eagerToTransactions = transactionService.getAccountTransactions("eager-to");

        assertThat(deposit.getToAccountId()).isEqualTo("eager-from");
        assertThat(transfer.getFromAccountId()).isEqualTo("eager-from");
        assertThat(withdrawal.getFromAccountId()).isEqualTo("eager-to");
        assertThat(eagerFromTransactions).hasSize(2);
        assertThat(eagerToTransactions).hasSize(2);
        assertThat(accountRepository.findById("eager-from").orElseThrow().getBalance()).isEqualByComparingTo(new BigDecimal("3897.00"));
        assertThat(accountRepository.findById("eager-to").orElseThrow().getBalance()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(notificationService.getNotificationsForClient("client-10")).hasSize(2);
        assertThat(notificationService.getNotificationsForClient("client-11")).hasSize(2);
    }

    @Test
    @Order(5)
    void withdraw_processedWithdrawnAndProvidesTransactionDetails() {
        accountRepository.save(account("weak-withdraw", "client-16", AccountType.CHECKING, Currency.USD, "1500.00", "5000.00"));

        Transaction transaction = transactionService.withdraw("weak-withdraw", new BigDecimal("100.00"), "ATM withdrawal");

        assertThat(transaction).isNotNull();
    }

    @Test
    @Order(6)
    void calculateTransferFee_correctlyCalculatesFeeViaReflection() throws Exception {
        Method calculateTransferFee = TransactionService.class.getDeclaredMethod("calculateTransferFee", BigDecimal.class);
        calculateTransferFee.setAccessible(true);

        BigDecimal fee = (BigDecimal) calculateTransferFee.invoke(transactionService, new BigDecimal("6000.00"));

        assertThat(fee).isEqualByComparingTo(new BigDecimal("50"));
    }


    private static Account account(String id, String clientId, AccountType type, Currency currency, String balance, String dailyLimit) {
        return Account.builder()
                .id(id)
                .clientId(clientId)
                .type(type)
                .currency(currency)
                .balance(new BigDecimal(balance))
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal(dailyLimit))
                .build();
    }
}
