package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.mockito.Mockito.*;

class SimpleExchangeRateServiceTest {

    private SimpleExchangeRateService exchangeRateService;
    private RateSource rateSource;

    /**
     * Setup executed before each test.
     * Creates a fresh instance to ensure test independence.
     */
    @BeforeEach
    void setUp() {
        rateSource = new RateSource() {
            @Override
            public Map<String, BigDecimal> loadRates() {
                Map<String, BigDecimal> rates = new HashMap<>();
                rates.put("USD_EUR", new BigDecimal("0.92"));
                rates.put("USD_UAH", new BigDecimal("41.50"));
                rates.put("EUR_UAH", new BigDecimal("45.00"));
                return rates;
            }
        };

        exchangeRateService = new SimpleExchangeRateService(rateSource);
    }

    @Nested
    class MainPart {
        /**
         * Demonstrates: Identity operation testing (edge case where input equals output)
         * FIRST principles: Fast (no external calls), Independent (no shared state)
         * <p>
         * Tests the special case where source and target currencies are the same.
         * Exchange rate should always be 1.0 for same currency conversion.
         * This is a mathematical identity property: X -> X = 1.
         */
        @Test
        void getExchangeRate_ForSameCurrency_ReturnsOne() {
            // Act
            BigDecimal rate = exchangeRateService.getExchangeRate(Currency.USD, Currency.USD);

            // Assert
            assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
        }

        /**
         * Demonstrates: Testing stateful services (service that maintains internal state)
         * FIRST principles: Independent (test doesn't affect others), Self-validating (clear assertion)
         * <p>
         * Tests that the service can update its internal state (exchange rates).
         * Verifies that setExchangeRate modifies the rate and getExchangeRate retrieves it correctly.
         * Stateful testing ensures the service maintains data correctly between operations.
         */
        @Test
        void setExchangeRate_UpdatesRate() {
            // Arrange
            BigDecimal newRate = new BigDecimal("1.00");

            // Act
            exchangeRateService.setExchangeRate(Currency.USD, Currency.EUR, newRate);
            BigDecimal result = exchangeRateService.getExchangeRate(Currency.USD, Currency.EUR);

            // Assert
            assertThat(result).isEqualByComparingTo(newRate);
        }

        /**
         * Demonstrates: Test isolation and repeatability through state cleanup
         * FIRST principles: Independent (tests don't interfere with each other),
         * Repeatable (can run in any order)
         * <p>
         * Tests that clearRates removes all stored rates, ensuring clean state.
         * This is crucial for test independence - each test should be able to run in isolation.
         * After clearing, attempting to get a rate should throw an exception.
         */
        @Test
        void clearRates_RemovesAllRates() {
            // Act
            exchangeRateService.clearRates();

            // Assert
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(Currency.USD, Currency.EUR))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void constructor_CallsLoadRatesOnProvidedRateSource() {
            // Arrange
            RateSource mockRateSource = mock(RateSource.class);
            when(mockRateSource.loadRates()).thenReturn(new HashMap<>());

            // Act
            new SimpleExchangeRateService(mockRateSource);

            // Assert
            verify(mockRateSource).loadRates();
        }
    }

    /**
     * Optional test cases for SimpleExchangeRateService - additional practice scenarios.
     */
    @Nested
    class OptionalPart {

        /**
         * Demonstrates: Currency conversion between different pairs
         * FIRST principles: Fast (no external API calls), Repeatable (fixed rates)
         * <p>
         * Tests direct currency conversion USD to EUR using loaded rates.
         * Uses the rate from file: USD_EUR=0.92
         * 100 USD * 0.92 = 92.00 EUR
         */
        @Test
        void convert_FromUsdToEur_ReturnsCorrectAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("100");

            // Act
            BigDecimal result = exchangeRateService.convert(amount, Currency.USD, Currency.EUR);

            // Assert
            assertThat(result).isEqualByComparingTo(new BigDecimal("92.00"));
        }

        /**
         * Demonstrates: Currency conversion with different currency pair
         * FIRST principles: Fast, Independent
         * <p>
         * Tests conversion USD to UAH using loaded rates.
         * Uses the rate from file: USD_UAH=41.50
         * 100 USD * 41.50 = 4150.00 UAH
         */
        @Test
        void convert_FromUsdToUah_ReturnsCorrectAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("100");

            // Act
            BigDecimal result = exchangeRateService.convert(amount, Currency.USD, Currency.UAH);

            // Assert
            assertThat(result).isEqualByComparingTo(new BigDecimal("4150.00"));
        }

        /**
         * Demonstrates: Currency conversion EUR to UAH
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests conversion from EUR to UAH.
         * Uses the rate from file: EUR_UAH=45.00
         * 100 EUR * 45.00 = 4500.00 UAH
         */
        @Test
        void convert_FromEurToUah_ReturnsCorrectAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("100");

            // Act
            BigDecimal result = exchangeRateService.convert(amount, Currency.EUR, Currency.UAH);

            // Assert
            assertThat(result).isEqualByComparingTo(new BigDecimal("4500.00"));
        }

        /**
         * Demonstrates: Reverse rate calculation (implicit inverse)
         * FIRST principles: Fast, Independent
         * <p>
         * Tests conversion EUR to USD using reverse rate calculation.
         * File has USD_EUR=0.92, so EUR_USD = 1/0.92 = 1.087 (approximately)
         * Service should automatically calculate reverse rate when needed.
         * 100 EUR * (1/0.92) = 108.70 USD
         */
        @Test
        void getExchangeRate_ForReverseRate_CalculatesCorrectly() {
            // Act
            BigDecimal rate = exchangeRateService.getExchangeRate(Currency.EUR, Currency.USD);

            // Assert
            // Should be approximately 1/0.92 = 1.086957
            assertThat(rate).isEqualByComparingTo(new BigDecimal("1.086957"));
        }

        /**
         * Demonstrates: Reverse rate calculation UAH to USD
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests reverse conversion UAH to USD.
         * File has USD_UAH=41.50, so UAH_USD = 1/41.50 = 0.024096
         * 100 UAH * (1/41.50) = 2.41 USD
         */
        @Test
        void convert_UsingReverseRate_ReturnsCorrectAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("100");

            // Act
            BigDecimal result = exchangeRateService.convert(amount, Currency.UAH, Currency.USD);

            // Assert
            // 100 * (1/41.50) = 2.41
            assertThat(result).isEqualByComparingTo(new BigDecimal("2.41"));
        }

        /**
         * Demonstrates: Rate consistency verification (mathematical property)
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that forward and reverse rates are mathematical inverses.
         * Property: rate(A->B) * rate(B->A) should equal 1.0
         * This verifies the reverse rate calculation is correct.
         */
        @Test
        void getExchangeRate_ForwardAndReverse_AreInverses() {
            // Act
            BigDecimal forwardRate = exchangeRateService.getExchangeRate(Currency.USD, Currency.EUR);
            BigDecimal reverseRate = exchangeRateService.getExchangeRate(Currency.EUR, Currency.USD);

            // Assert
            BigDecimal product = forwardRate.multiply(reverseRate);
            // Use tolerance for floating point precision (should be very close to 1.0)
            assertThat(product.doubleValue()).isCloseTo(1.0, within(0.0001));
        }

        /**
         * Demonstrates: Null amount parameter validation
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that null amount is rejected in convert operation.
         * Amount is required for conversion - null would cause NullPointerException.
         */
        @Test
        void convert_WithNullAmount_ThrowsException() {
            // Arrange
            BigDecimal nullAmount = null;

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.convert(nullAmount, Currency.USD, Currency.EUR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be null");
        }

        /**
         * Demonstrates: Negative amount validation
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that negative amounts are rejected in conversion.
         * Business rule: cannot convert negative amounts (no negative money).
         */
        @Test
        void convert_WithNegativeAmount_ThrowsException() {
            // Arrange
            BigDecimal negativeAmount = new BigDecimal("-100");

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.convert(negativeAmount, Currency.USD, Currency.EUR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amount cannot be negative");
        }

        /**
         * Demonstrates: Zero amount conversion (edge case)
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests that converting zero amount returns zero.
         * Mathematical property: 0 * rate = 0
         * Zero is non-negative, so it's valid input.
         */
        @Test
        void convert_WithZeroAmount_ReturnsZero() {
            // Arrange
            BigDecimal zeroAmount = BigDecimal.ZERO;

            // Act
            BigDecimal result = exchangeRateService.convert(zeroAmount, Currency.USD, Currency.EUR);

            // Assert
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * Demonstrates: Null currency parameter validation in setExchangeRate
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that null source currency is rejected when setting rate.
         * All parameters must be non-null for rate setting operation.
         */
        @Test
        void setExchangeRate_WithNullFromCurrency_ThrowsException() {
            // Arrange
            Currency nullFrom = null;
            BigDecimal rate = new BigDecimal("1.5");

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.setExchangeRate(nullFrom, Currency.EUR, rate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parameters cannot be null");
        }

        /**
         * Demonstrates: Null target currency validation in setExchangeRate
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that null target currency is rejected when setting rate.
         * Complements null source currency test.
         */
        @Test
        void setExchangeRate_WithNullToCurrency_ThrowsException() {
            // Arrange
            Currency nullTo = null;
            BigDecimal rate = new BigDecimal("1.5");

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.setExchangeRate(Currency.USD, nullTo, rate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parameters cannot be null");
        }

        /**
         * Demonstrates: Null rate validation in setExchangeRate
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that null rate value is rejected when setting rate.
         * Rate is the core data being set - cannot be null.
         */
        @Test
        void setExchangeRate_WithNullRate_ThrowsException() {
            // Arrange
            BigDecimal nullRate = null;

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.setExchangeRate(Currency.USD, Currency.EUR, nullRate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Parameters cannot be null");
        }

        /**
         * Demonstrates: Negative rate validation
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that negative exchange rates are rejected.
         * Business rule: exchange rates must be positive (no negative money value).
         */
        @Test
        void setExchangeRate_WithNegativeRate_ThrowsException() {
            // Arrange
            BigDecimal negativeRate = new BigDecimal("-1.5");

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.setExchangeRate(Currency.USD, Currency.EUR, negativeRate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exchange rate must be positive");
        }

        /**
         * Demonstrates: Zero rate validation
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that zero exchange rates are rejected.
         * Business rule: rates must be positive - zero rate is invalid
         * (would mean target currency has no value).
         */
        @Test
        void setExchangeRate_WithZeroRate_ThrowsException() {
            // Arrange
            BigDecimal zeroRate = BigDecimal.ZERO;

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.setExchangeRate(Currency.USD, Currency.EUR, zeroRate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exchange rate must be positive");
        }

        /**
         * Demonstrates: Null currency validation in getExchangeRate
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that null source currency is rejected when getting rate.
         * Both currencies must be specified to look up rate.
         */
        @Test
        void getExchangeRate_WithNullFromCurrency_ThrowsException() {
            // Arrange
            Currency nullFrom = null;

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(nullFrom, Currency.EUR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currencies cannot be null");
        }

        /**
         * Demonstrates: Null target currency validation in getExchangeRate
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that null target currency is rejected when getting rate.
         * Complements null source currency test for getExchangeRate.
         */
        @Test
        void getExchangeRate_WithNullToCurrency_ThrowsException() {
            // Arrange
            Currency nullTo = null;

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(Currency.USD, nullTo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currencies cannot be null");
        }

        /**
         * Demonstrates: Missing rate exception handling
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that requesting unavailable rate throws exception.
         * After clearRates(), no rates exist - service should fail gracefully.
         * This is different from reverse rate calculation - no rate exists at all.
         */
        @Test
        void getExchangeRate_AfterClearRates_ThrowsException() {
            // Arrange
            exchangeRateService.clearRates();

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(Currency.USD, Currency.EUR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exchange rate not available");
        }

        /**
         * Demonstrates: Currency conversion chain scenario
         * FIRST principles: Fast, Repeatable
         * <p>
         * Tests converting USD -> EUR -> UAH and comparing with direct USD -> UAH.
         * This verifies rate consistency across currency chain.
         * Due to rounding, chained conversion might differ slightly from direct.
         * USD -> EUR: 100 * 0.92 = 92
         * EUR -> UAH: 92 * 45 = 4140
         * Direct USD -> UAH: 100 * 41.50 = 4150
         * Small difference (4140 vs 4150) due to exchange rate arbitrage.
         */
        @Test
        void convert_ChainedConversion_ProducesReasonableResult() {
            // Arrange
            BigDecimal amount = new BigDecimal("100");

            // Act
            BigDecimal usdToEur = exchangeRateService.convert(amount, Currency.USD, Currency.EUR);
            BigDecimal eurToUah = exchangeRateService.convert(usdToEur, Currency.EUR, Currency.UAH);
            BigDecimal directUsdToUah = exchangeRateService.convert(amount, Currency.USD, Currency.UAH);

            // Assert
            // Chained: 100 -> 92.00 EUR -> 4140.00 UAH
            assertThat(eurToUah).isEqualByComparingTo(new BigDecimal("4140.00"));
            // Direct: 100 -> 4150.00 UAH
            assertThat(directUsdToUah).isEqualByComparingTo(new BigDecimal("4150.00"));
            // They differ due to rate precision, both are valid
        }

        /**
         * Demonstrates: Null currency validation in convert
         * FIRST principles: Fast, Self-validating
         * <p>
         * Tests that null source currency is rejected in convert operation.
         * Convert internally calls getExchangeRate which validates currencies.
         */
        @Test
        void convert_WithNullFromCurrency_ThrowsException() {
            // Arrange
            BigDecimal amount = new BigDecimal("100");
            Currency nullFrom = null;

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.convert(amount, nullFrom, Currency.EUR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currencies cannot be null");
        }

        /**
         * Demonstrates: Null target currency validation in convert
         * FIRST principles: Fast, Independent
         * <p>
         * Tests that null target currency is rejected in convert operation.
         * Complements null source currency test for convert.
         */
        @Test
        void convert_WithNullToCurrency_ThrowsException() {
            // Arrange
            BigDecimal amount = new BigDecimal("100");
            Currency nullTo = null;

            // Act & Assert
            assertThatThrownBy(() -> exchangeRateService.convert(amount, Currency.USD, nullTo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Currencies cannot be null");
        }
    }
}