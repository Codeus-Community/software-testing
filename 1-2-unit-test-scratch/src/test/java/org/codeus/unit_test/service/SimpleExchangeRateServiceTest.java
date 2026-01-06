package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    /**
     * Demonstrates: Identity operation testing (edge case where input equals output)
     * FIRST principles: Fast (no external calls), Independent (no shared state)
     *
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
     *
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
     *                   Repeatable (can run in any order)
     *
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