# Unit Tests: refactoring unit tests

## Goal

Practice refactoring unit tests so they become easier to understand, faster, more deterministic, and less coupled to implementation details.

You will work with two test classes:

- [SimpleExchangeRateServiceTest.java](./src/test/java/org/codeus/unit_test/service/SimpleExchangeRateServiceTest.java) as a warm-up
- [TransactionServiceTest.java](./src/test/java/org/codeus/unit_test/service/TransactionServiceTest.java)

Start with `SimpleExchangeRateServiceTest` first as a good warm-up. Once finished, switch to `TransactionServiceTest`.

## High-level steps

1. Open [SimpleExchangeRateServiceTest.java](./src/test/java/org/codeus/unit_test/service/SimpleExchangeRateServiceTest.java).
2. Read the `TODO` and analyze what makes the current tests hard to maintain or trust.
3. Refactor the tests. Keep in mind that the final number of tests may differ from the original set: some tests can be split, merged, or rewritten.
4. Move on to [TransactionServiceTest.java](./src/test/java/org/codeus/unit_test/service/TransactionServiceTest.java) and repeat the same process.
5. Switch to the `master-completed` branch and review the completed solution.
6. Compare your refactoring with the completed solution and note the differences in structure, naming, isolation, and assertions.

## Hints

### SimpleExchangeRateServiceTest
<details>
<summary>Hint for test 1 in <code>SimpleExchangeRateServiceTest</code></summary>

Tests 1 and 2 depend on shared state and execution order: one test prepares data and another one relies on it. Refactor them so each test creates its own service and data, then asserts the exact expected result.

</details>

<details>
<summary>Hint for test 2 in <code>SimpleExchangeRateServiceTest</code></summary>

Tests 1 and 2 depend on shared state and execution order: one test prepares data and another one relies on it. Refactor them so each test creates its own service and data, then asserts the exact expected result.

</details>

<details>
<summary>Hint for test 3 in <code>SimpleExchangeRateServiceTest</code></summary>

This test is coupled to the file system and another component. Replace the file-based setup with in-memory test data so the test stays focused, fast, and deterministic.

</details>

<details>
<summary>Hint for test 4 in <code>SimpleExchangeRateServiceTest</code></summary>

The main issue here is naming. Keep the test behavior the same, but rename it so the scenario and expected outcome are immediately clear.

</details>

<details>
<summary>Hint for test 5 in <code>SimpleExchangeRateServiceTest</code></summary>

This test uses reflection to verify internal state. Refactor it to check observable behavior through the public API instead of inspecting private fields.

</details>

### TransactionServiceTest

<details>
<summary>Hint for test 1 in <code>TransactionServiceTest</code></summary>

Tests 1 and 2 are order-dependent and rely on shared repository state created by earlier tests. Refactor them so each test sets up only the accounts and state it needs, without depending on another test run before it.

</details>

<details>
<summary>Hint for test 2 in <code>TransactionServiceTest</code></summary>

Tests 1 and 2 are order-dependent and rely on shared repository state created by earlier tests. Refactor them so each test sets up only the accounts and state it needs, without depending on another test run before it.

</details>

<details>
<summary>Hint for test 3 in <code>TransactionServiceTest</code></summary>

This test is slower and more coupled than a unit test should be because it brings in an online-style exchange rate dependency. Use a local stub or test double so the test remains fast, predictable, and isolated.

</details>

<details>
<summary>Hint for test 4 in <code>TransactionServiceTest</code></summary>

This is an eager test: it checks deposit, transfer, withdrawal, history, balances, and notifications all at once. Split it into smaller focused tests so each one verifies a single behavior and has one clear reason to fail.

</details>

<details>
<summary>Hint for test 5 in <code>TransactionServiceTest</code></summary>

The assertion is too weak. Instead of only checking that the returned transaction is not null, verify the important transaction details and the resulting account state.

</details>

<details>
<summary>Hint for test 6 in <code>TransactionServiceTest</code></summary>

This test reaches into a private method through reflection. Refactor it to verify fee-related behavior through the public API, so the test describes business behavior rather than implementation details.

</details>
