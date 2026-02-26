package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.AccountStatus;
import org.codeus.unit_test.enums.AccountType;
import org.codeus.unit_test.enums.Currency;
import org.codeus.unit_test.exception.FraudDetectedException;
import org.codeus.unit_test.model.Account;
import org.codeus.unit_test.repository.AccountRepository;
import org.codeus.unit_test.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionValidator transactionValidator;

    @Mock
    private NotificationService notificationService;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private AccountService accountService;

    private LocalDateTime fixedTime;

    /**
     * Initializes a fixed time to ensure test repeatability (FIRST - Repeatable).
     * Using a fixed time instead of LocalDateTime.now() makes tests deterministic
     * and independent of when they are executed.
     */
    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.of(2024, 1, 15, 10, 0);
    }

    private Account createAccount(String accountId, BigDecimal balance) {
        return Account.builder()
                .id(accountId)
                .clientId("client-001")
                .type(AccountType.CHECKING)
                .currency(Currency.USD)
                .balance(balance)
                .status(AccountStatus.ACTIVE)
                .dailyWithdrawalLimit(new BigDecimal("10000"))
                .build();
    }

    @Nested
    class MainPart {
        /**
         * Tests the basic account creation flow with valid data.
         * Shows how to mock dependencies and verify the created account properties.
         */
        // TODO: implement test

        /**
         * Tests deposit operation and verifies that all dependencies are called correctly.
         * Shows how to verify mock interactions with validator, repository, and notification service.
         */
        // TODO: implement test

        /**
         * Tests withdrawal that results in low balance and verifies that alert is sent.
         * Shows how to test edge cases where balance crosses a threshold (LOW_BALANCE_THRESHOLD).
         */
        // TODO: implement test

        /**
         * Tests that suspicious deposits are detected and rejected with FraudDetectedException.
         * Shows how to test exception scenarios and verify that certain operations are NOT called.
         */
        // TODO: implement test

        /**
         * Tests that closing an account with positive balance is not allowed.
         * Shows how to test business rules and verify that invalid operations throw exceptions.
         */
        // TODO: implement test
    }

    /**
     * Optional test cases for AccountService - additional practice scenarios.
     */
    @Nested
    class OptionalPart {

        /**
         * Tests that different account types are created with correct default daily limits.
         * Each account type has specific withdrawal limits based on business rules:
         * - SAVINGS: 5000 (conservative limit for savings protection)
         * - CHECKING: 10000 (higher limit for everyday transactions)
         * - BUSINESS: 50000 (highest limit for business operations)
         */
        @ParameterizedTest
        @MethodSource("provideAccountTypesAndLimits")
        void createAccount_WithDifferentAccountTypes_SetsCorrectDailyLimit() {
            // TODO: implement test
        }

        private static Stream<Arguments> provideAccountTypesAndLimits() {
            return Stream.of(
                    Arguments.of(AccountType.SAVINGS, new BigDecimal("5000")),
                    Arguments.of(AccountType.CHECKING, new BigDecimal("10000")),
                    Arguments.of(AccountType.BUSINESS, new BigDecimal("50000"))
            );
        }

        /**
         * Tests account creation with different supported currencies.
         * Verifies that the system correctly handles USD, EUR, and UAH currencies.
         * This ensures multi-currency support works for account creation.
         */
        @ParameterizedTest
        @EnumSource(Currency.class)
        void createAccount_WithDifferentCurrencies_CreatesAccountSuccessfully() {
            // TODO: implement test
        }

        /**
         * Tests that null clientId is properly rejected with IllegalArgumentException.
         * Input validation is critical - the service must reject invalid inputs before
         * any business logic executes or data is persisted.
         */
        @Test
        void createAccount_WithNullClientId_ThrowsException() {
            // TODO: implement test
        }

        /**
         * Tests that null account type is rejected before account creation.
         * This is part of defensive programming - validate all inputs early.
         */
        @Test
        void createAccount_WithNullAccountType_ThrowsException() {
            // TODO: implement test
        }

        /**
         * Tests that null currency is rejected during account creation.
         * Currency is required for all accounts - without it, balance has no meaning.
         */
        @Test
        void createAccount_WithNullCurrency_ThrowsException() {
            // TODO: implement test
        }

        /**
         * Tests that negative initial deposits are rejected.
         * Business rule: deposits must be non-negative. Negative amounts would
         * create accounts with debt, which violates account creation rules.
         */
        @Test
        void createAccount_WithNegativeInitialDeposit_ThrowsException() {
            // TODO: implement test
        }

        /**
         * Tests that accounts can be created with zero balance.
         * This is a valid edge case - users should be able to open empty accounts.
         * Zero is non-negative, so it passes validation.
         */
        @Test
        void createAccount_WithZeroInitialDeposit_CreatesAccountSuccessfully() {
            // TODO: implement test
        }

        /**
         * Tests that null initial deposit defaults to zero balance.
         * Business rule: if no initial deposit specified, create account with zero balance.
         * This is user-friendly - users don't have to explicitly pass zero.
         */
        @Test
        void createAccount_WithNullInitialDeposit_CreatesAccountWithZeroBalance() {
            // TODO: implement test
        }

        /**
         * Tests boundary condition where withdrawal leaves exactly 100 (threshold).
         * Balance = threshold should NOT trigger low balance alert (only < threshold).
         * This tests the boundary: alert triggers at 99.99, but not at 100.00.
         */
        @Test
        void withdraw_LeavingExactThresholdBalance_DoesNotSendAlert() {
            // TODO: implement test
        }

        /**
         * Tests boundary condition where withdrawal leaves 99.99 (just below 100 threshold).
         * This should trigger low balance alert as balance < LOW_BALANCE_THRESHOLD (100).
         * Complements previous test - verifies alert triggers at 99.99 but not at 100.00.
         */
        @Test
        void withdraw_LeavingBalanceJustBelowThreshold_SendsAlert() {
            // TODO: implement test
        }

        /**
         * Tests that active accounts can be successfully blocked.
         * Blocking is an administrative action - freezes account operations
         * without closing the account permanently.
         */
        @Test
        void blockAccount_WithActiveAccount_BlocksSuccessfully() {
            // TODO: implement test
        }

        /**
         * Tests that blocked accounts can be unblocked back to active status.
         * This reverses the blocking operation - allows account to resume operations.
         */
        @Test
        void unblockAccount_WithBlockedAccount_UnblocksSuccessfully() {
            // TODO: implement test
        }

        /**
         * Tests that attempting to unblock an active account throws exception.
         * Business rule: can only unblock accounts that are currently blocked.
         * This prevents misuse of the unblock operation.
         */
        @Test
        void unblockAccount_WithActiveAccount_ThrowsException() {
            // TODO: implement test
        }

        /**
         * Tests that accounts with zero balance can be closed.
         * Business rule: accounts must have zero balance before closure.
         * This is standard banking practice - settle all balances before closing.
         */
        @Test
        void closeAccount_WithZeroBalance_ClosesSuccessfully() {
            // TODO: implement test
        }

        /**
         * Tests that depositing to non-existent account throws exception.
         * Repository returns Optional.empty() for missing accounts.
         * Service must handle this gracefully with clear error message.
         */
        @Test
        void deposit_WithNonExistentAccount_ThrowsException() {
            // TODO: implement test
        }

        /**
         * Tests that withdrawing from non-existent account throws exception.
         * Similar to deposit test, but for withdrawal operation.
         * Both must fail early with clear error when account doesn't exist.
         */
        @Test
        void withdraw_WithNonExistentAccount_ThrowsException() {
            // TODO: implement test
        }

        /**
         * Tests that suspicious withdrawals are detected and rejected.
         * Similar to deposit fraud test, but for withdrawal operations.
         * Both operations must be monitored for fraudulent activity.
         */
        @Test
        void withdraw_WithSuspiciousAmount_ThrowsFraudException() {
            // TODO: implement test
        }
    }
}