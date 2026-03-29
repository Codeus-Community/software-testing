package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

//TODO:
// - analyze all test cases below
// - refactor the test cases using information from the presentation (FIRST principles, best practices... and common sense)
// _____
// Note: the number of refactored test cases can differ from this original tests. E.g. some of tests may be merged or split.

@TestMethodOrder(OrderAnnotation.class)
class SimpleExchangeRateServiceTest {

    private static SimpleExchangeRateService sharedService;

    @BeforeAll
    static void setUpSuite() {
        sharedService = new SimpleExchangeRateService(() -> Map.of(
            "USD_EUR", new BigDecimal("0.92"),
            "USD_UAH", new BigDecimal("41.50")
        ));
    }

    @Test
    @Order(1)
    void setExchangeRate_setsExchangeRateSuccessfullyAndPreparesContextForOtherTests() {
        sharedService.setExchangeRate(Currency.EUR, Currency.UAH, new BigDecimal("45.00"));
        System.out.println("Successfully set EUR_UAH exchange rate");
    }

    @Test
    @Order(2)
    void getExchangeRate_returnsConfiguredExchangeRate() {
        BigDecimal result = sharedService.getExchangeRate(Currency.EUR, Currency.UAH);
        System.out.println("Exchange rate for EUR_UAH should be 45.00, actual = " + result
          + " is passed = " + result.equals(new BigDecimal("45.00")));
    }

    @Test
    @Order(3)
    void convert_returnsConvertedAmountUsingConfiguredRateFile() throws Exception {
        Path ratesFile = Files.createTempFile("exchange-rates", ".txt");
        Files.writeString(ratesFile, "USD_EUR=0.92\n");

        SimpleExchangeRateService fileBasedService = new SimpleExchangeRateService(new FileRateSource(ratesFile.toString()));
        BigDecimal converted = fileBasedService.convert(new BigDecimal("100.00"), Currency.USD, Currency.EUR);

        System.out.println("Exchange rate for EUR_UAH should be 92.00, actual = " + converted
          + " is passed = " + converted.equals(new BigDecimal("92.00")));
    }

    @Test
    @Order(4)
    void convert_fails() {
        SimpleExchangeRateService exchangeRateService = new SimpleExchangeRateService(() -> Map.of("USD_EUR", new BigDecimal("0.92")));

        assertThrows(
          IllegalArgumentException.class,
          () -> exchangeRateService.convert(null, Currency.USD, Currency.EUR),
          "Amount cannot be null");
    }

    @Test
    @Order(5)
    void setExchangeRate_storesConfiguredRateInServiceState() throws Exception {
        SimpleExchangeRateService service = new SimpleExchangeRateService(HashMap::new);
        service.setExchangeRate(Currency.USD, Currency.UAH, new BigDecimal("41.80"));

        Field exchangeRatesField = SimpleExchangeRateService.class.getDeclaredField("exchangeRates");
        exchangeRatesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> exchangeRates = (Map<String, BigDecimal>) exchangeRatesField.get(service);

        assertThat(exchangeRates.get("USD_UAH"))
                .isEqualByComparingTo(new BigDecimal("41.80"));
    }
}
