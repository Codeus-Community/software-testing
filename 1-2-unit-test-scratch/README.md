
## Overview

A simple banking system that demonstrates core financial operations:
account management, deposits, withdrawals, transfers, interest calculation,
and fraud detection. Your task is to write unit tests for this system.

## System Components

### Models
- **Account** - bank account with balance, currency, status and daily withdrawal limit
- **Transaction** - financial operation record (deposit, withdrawal, transfer)
- **Client** - bank customer

### Services
- **AccountService** - account lifecycle management: create, deposit, withdraw, block, close
- **TransactionService** - transaction processing: transfer between accounts, deposit, withdrawal with transaction recording
- **TransactionValidator** - validates business rules before any transaction
- **InterestCalculator** - calculates simple and compound interest for accounts
- **SimpleExchangeRateService** - currency conversion between USD, EUR, UAH
- **SimpleFraudDetectionService** - detects suspicious transactions based on amount thresholds

### Utilities
- **FixedTimeProvider** - deterministic time provider for testing (instead of `LocalDateTime.now()`)
- **SystemTimeProvider** - real system time for production use

---

## Test Structure

Each test class contains two nested classes:

### `MainPart`
Core test cases that cover the most important business logic.
**Your task:** implement each test case described by the Javadoc comment above `// TODO: implement test`.
- Come up with a meaningful test method name following the pattern: `methodName_Condition_ExpectedResult`
- Write the full test body using the AAA pattern (Arrange, Act, Assert)
- Use mocks where appropriate (classes annotated with `@Mock` and `@InjectMocks` are already provided)

### `OptionalPart`
Additional test cases for deeper practice. Method names and signatures are already provided.
**Your task:** implement the test body for each method marked with `// TODO: implement test`.

---

## Test Cases

## FixedTimeProvider
### Main Part
Implement the following test cases:
- Returns fixed time on `now()` call.
- Changes returned time after `setTime()` call.

### Optional Part
- Multiple `setTime()` calls update time correctly.
- Multiple `now()` calls return the same value.
- Advancing time simulates time progression.
- Backward time movement updates correctly.
- Epoch time is handled correctly.
- Far future time is handled correctly.
- Two independent providers with different times.
- Nanosecond precision is maintained.
- Midnight boundary is handled correctly.
- End of day boundary is handled correctly.
- Year boundary (New Year) is handled correctly.
- Leap year date is handled correctly.

---

## InterestCalculator
### Main Part
Implement the following test cases:
- Simple interest calculation for a 30-day period.
- Interest calculation for zero days returns zero.

### Optional Part
- Interest calculation for different account types (parametrized).
- Interest calculation for 1 day.
- Interest calculation for 90 days.
- Interest calculation for multiple years.
- Compound interest with monthly compounding.
- Compound interest with quarterly compounding.
- Compound interest with daily compounding.
- Null account throws exception.
- Null start date throws exception.
- Null end date throws exception.
- End date before start date throws exception.
- Zero balance returns zero interest.
- Zero balance returns zero compound interest.
- Zero compounding periods throws exception.
- Negative compounding periods throws exception.
- Compound interest for zero days returns zero.
- Null account type throws exception in `getAnnualRate`.
- Correct annual rates for all account types.

---

## SimpleExchangeRateService
### Main Part
Implement the following test cases:
- Same currency exchange rate returns 1.
- `setExchangeRate()` updates the rate correctly.
- `clearRates()` removes all rates.
- Constructor calls `loadRates()` on provided `RateSource`.

### Optional Part
- Convert USD to EUR returns correct amount.
- Convert USD to UAH returns correct amount.
- Convert EUR to UAH returns correct amount.
- Reverse rate is calculated correctly.
- Reverse rate conversion returns correct amount.
- Forward and reverse rates are mathematical inverses.
- Null amount throws exception.
- Negative amount throws exception.
- Zero amount returns zero.
- Null source currency in `setExchangeRate` throws exception.
- Null target currency in `setExchangeRate` throws exception.
- Null rate in `setExchangeRate` throws exception.
- Negative rate throws exception.
- Zero rate throws exception.
- Null source currency in `getExchangeRate` throws exception.
- Null target currency in `getExchangeRate` throws exception.
- After `clearRates()` getting rate throws exception.
- Chained conversion produces reasonable result.
- Null source currency in `convert` throws exception.
- Null target currency in `convert` throws exception.

---

## TransactionValidator
### Main Part
Implement the following test cases:
- Valid withdrawal does not throw exception.
- Insufficient funds throws exception.

### Optional Part
- Blocked account throws exception on withdrawal.
- Closed account throws exception on withdrawal.
- Blocked account throws exception on deposit.
- Closed account throws exception on deposit.
- Transfer to same account throws exception.
- Transfer with insufficient funds throws exception.
- Transfer from blocked account throws exception.
- Transfer to blocked account throws exception.
- Transfer to closed account throws exception.
- Daily limit exactly exceeded throws exception.
- Daily limit slightly exceeded throws exception.
- Daily limit far exceeded throws exception.
- Just under daily limit does not throw exception.
- Null daily limit does not throw exception.
- Null account in withdrawal throws exception.
- Null amount in withdrawal throws exception.
- Zero amount in withdrawal throws exception.
- Negative amount in withdrawal throws exception.
- Null account in deposit throws exception.
- Null amount in deposit throws exception.
- Zero amount in deposit throws exception.
- Negative amount in deposit throws exception.
- Valid deposit does not throw exception.
- Valid transfer does not throw exception.
- Null source account in transfer throws exception.
- Null destination account in transfer throws exception.
- Null amount in transfer throws exception.

---

## AccountService
### Main Part
Implement the following test cases:
- Account creation with valid data creates and returns account.
- Deposit with valid data increases balance.
- Withdrawal resulting in low balance sends alert.
- Suspicious deposit throws fraud exception.
- Closing account with positive balance throws exception.

### Optional Part
- Different account types have correct daily limits (parametrized).
- Account creation with different currencies (parametrized).
- Null client ID throws exception.
- Null account type throws exception.
- Null currency throws exception.
- Negative initial deposit throws exception.
- Zero initial deposit creates account successfully.
- Null initial deposit creates account with zero balance.
- Withdrawal leaving exact threshold balance does not send alert.
- Withdrawal leaving balance just below threshold sends alert.
- Blocking active account succeeds.
- Unblocking blocked account succeeds.
- Unblocking active account throws exception.
- Closing account with zero balance succeeds.
- Deposit to non-existent account throws exception.
- Withdrawal from non-existent account throws exception.
- Suspicious withdrawal throws fraud exception.

---

## TransactionService
### Main Part
Implement the following test cases:
- Transfer with valid data transfers money successfully.
- Transfer operations occur in correct order.
- Suspicious transfer throws exception and does not save.
- Withdrawal near daily limit sends warning.

### Optional Part
- Transfer between different currencies converts amount correctly.
- Multiple withdrawals tracking daily limit.
- Transfer calculates fee correctly for medium amount.
- Transfer applies minimum fee for small amount.
- Transfer applies maximum fee for large amount.
- Deposit with valid data creates transaction.
- Withdrawal with valid data creates transaction.
- Get account transactions returns all transactions.
- Get account transactions by date range returns filtered results.
- Get account transactions with no transactions returns empty list.
- Transfer with same currency does not call exchange service.
- Transfer sends notifications to both accounts.
- Withdrawal well below daily limit does not send warning.
- Transfer with different currencies calls exchange service with correct parameters.