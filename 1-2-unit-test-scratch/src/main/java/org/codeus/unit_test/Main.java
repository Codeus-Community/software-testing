package org.codeus.unit_test;

import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.model.Transaction;
import org.codeus.unit_test.repository.AccountRepository;
import org.codeus.unit_test.repository.InMemoryAccountRepository;
import org.codeus.unit_test.repository.InMemoryTransactionRepository;
import org.codeus.unit_test.repository.TransactionRepository;
import org.codeus.unit_test.service.*;
import org.codeus.unit_test.util.SystemTimeProvider;
import org.codeus.unit_test.util.TimeProvider;

import java.math.BigDecimal;

public class Main {
  public static void main(String[] args) {
    System.out.println("=== Banking System Demo ===\n");

    AccountRepository accountRepository = new InMemoryAccountRepository();
    TransactionRepository transactionRepository = new InMemoryTransactionRepository();
    TimeProvider timeProvider = new SystemTimeProvider();

    TransactionValidator validator = new TransactionValidator();
    NotificationService notificationService = new SimpleNotificationService();
    FraudDetectionService fraudDetectionService = new SimpleFraudDetectionService(notificationService);
    RateSource rateSource = new FileRateSource("1-2-unit-test-scratch/src/main/data/exchange-rates.txt");
    ExchangeRateService exchangeRateService = new SimpleExchangeRateService(rateSource);

    AccountService accountService = new AccountService(
            accountRepository, validator, notificationService, fraudDetectionService
    );

    TransactionService transactionService = new TransactionService(
            accountRepository, transactionRepository, validator, fraudDetectionService,
            notificationService, exchangeRateService, timeProvider
    );

    try {
      System.out.println("1. Creating accounts...");
      Account savingsAccount = accountService.createAccount(
              "client-001", AccountType.SAVINGS, Currency.USD, new BigDecimal("1000")
      );
      System.out.println("   Created: " + savingsAccount.getType() + " account with balance: $"
              + savingsAccount.getBalance());

      Account checkingAccount = accountService.createAccount(
              "client-001", AccountType.CHECKING, Currency.EUR, new BigDecimal("500")
      );
      System.out.println("   Created: " + checkingAccount.getType() + " account with balance: €"
              + checkingAccount.getBalance());

      System.out.println("\n2. Making a deposit...");
      Account updatedAccount = accountService.deposit(savingsAccount.getId(), new BigDecimal("200"));
      System.out.println("   New balance: $" + updatedAccount.getBalance());

      System.out.println("\n3. Making a withdrawal...");
      updatedAccount = accountService.withdraw(savingsAccount.getId(), new BigDecimal("300"));
      System.out.println("   New balance: $" + updatedAccount.getBalance());

      System.out.println("\n4. Transferring money between accounts...");
      Transaction transfer = transactionService.transfer(
              savingsAccount.getId(),
              checkingAccount.getId(),
              new BigDecimal("100")
      );
      System.out.println("   Transfer completed! Fee: $" + transfer.getFee());

      Account fromAccount = accountService.getAccount(savingsAccount.getId());
      Account toAccount = accountService.getAccount(checkingAccount.getId());
      System.out.println("   From account balance: $" + fromAccount.getBalance());
      System.out.println("   To account balance: €" + toAccount.getBalance());

      System.out.println("\n5. Calculating interest...");
      InterestCalculator calculator = new InterestCalculator();
      BigDecimal interest = calculator.calculateInterest(
              fromAccount,
              timeProvider.now(),
              timeProvider.now().plusDays(30)
      );
      System.out.println("   Interest for 30 days: $" + interest);

      System.out.println("\n6. Testing fraud detection...");
      try {
        accountService.deposit(savingsAccount.getId(), new BigDecimal("50000"));
      } catch (Exception e) {
        System.out.println("   Fraud detected: " + e.getMessage());
      }

      System.out.println("\n7. Testing daily limit...");
      System.out.println("   Daily withdrawal limit: $" + fromAccount.getDailyWithdrawalLimit());

      System.out.println("\n=== Demo completed successfully! ===");

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }
}