package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleExchangeRateServiceTest {

    /**
     * Refactor of the shared-state bad tests (tests 1 and 2):
     * create a fresh service in the test so the result does not depend on execution order,
     * and assert the exact result so the test stays fast and self-validating.
     */
    @Test
    void getExchangeRate_readsTheRateWithoutDependingOnAnotherTest() {
        SimpleExchangeRateService service = new SimpleExchangeRateService(() -> {
            Map<String, BigDecimal> rates = new HashMap<>();
            rates.put("USD_EUR", new BigDecimal("0.92"));
            rates.put("EUR_UAH", new BigDecimal("45.00"));
            return rates;
        });

        BigDecimal result = service.getExchangeRate(Currency.EUR, Currency.UAH);

        assertThat(result).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    /**
     * Refactor of the local environment and another component coupling bad test (tests 3):
     * keep all test data in memory and assert the exact result so the test stays fast and self-validating.
     */
    @Test
    void convert_usesInMemoryRatesAndClearAssertions() {
        SimpleExchangeRateService service = new SimpleExchangeRateService(() -> {
            Map<String, BigDecimal> rates = new HashMap<>();
            rates.put("USD_EUR", new BigDecimal("0.92"));
            return rates;
        });

        BigDecimal converted = service.convert(new BigDecimal("100.00"), Currency.USD, Currency.EUR);

        assertThat(converted).isEqualByComparingTo(new BigDecimal("92.00"));
    }

    /**
     * Refactor of bad test with bad naming (test 4):
     * make test case name concise but descriptive enough;
     * agree with your team on test case naming convention and use it consistently
     */
    @Test
    void convert_throwsException_whenAmountIsNull() {
        SimpleExchangeRateService exchangeRateService = new SimpleExchangeRateService(() -> Map.of("USD_EUR", new BigDecimal("0.92")));

        assertThrows(
          IllegalArgumentException.class,
          () -> exchangeRateService.convert(null, Currency.USD, Currency.EUR),
          "Amount cannot be null");
    }

    /**
     * Refactor of the reflection-based bad test (test 5):
     * verify the observable contract through the public API, which makes the test less brittle during refactors.
     */
    @Test
    void setExchangeRate_exposesTheUpdatedRateThroughPublicApi() {
        SimpleExchangeRateService service = new SimpleExchangeRateService(new RateSource() {
            @Override
            public Map<String, BigDecimal> loadRates() {
                return new HashMap<>();
            }
        });

        service.setExchangeRate(Currency.USD, Currency.UAH, new BigDecimal("41.80"));

        assertThat(service.getExchangeRate(Currency.USD, Currency.UAH))
                .isEqualByComparingTo(new BigDecimal("41.80"));
    }
}
