package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleExchangeRateServiceTest {

    private SimpleExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new SimpleExchangeRateService();
    }

    @Test
    void getExchangeRate_ForSameCurrency_ReturnsOne() {
        // Act
        BigDecimal rate = exchangeRateService.getExchangeRate(Currency.USD, Currency.USD);

        // Assert
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void getExchangeRate_FromUsdToEur_ReturnsCorrectRate() {
        // Act
        BigDecimal rate = exchangeRateService.getExchangeRate(Currency.USD, Currency.EUR);

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("0.92"));
    }

    @Test
    void getExchangeRate_FromUsdToUah_ReturnsCorrectRate() {
        // Act
        BigDecimal rate = exchangeRateService.getExchangeRate(Currency.USD, Currency.UAH);

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("41.50"));
    }

    @Test
    void getExchangeRate_FromEurToUah_ReturnsCorrectRate() {
        // Act
        BigDecimal rate = exchangeRateService.getExchangeRate(Currency.EUR, Currency.UAH);

        // Assert
        assertThat(rate).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    @Test
    void getExchangeRate_ReverseDirection_CalculatesCorrectly() {
        // Arrange
        BigDecimal directRate = exchangeRateService.getExchangeRate(Currency.USD, Currency.EUR);

        // Act
        BigDecimal reverseRate = exchangeRateService.getExchangeRate(Currency.EUR, Currency.USD);

        // Assert
        assertThat(reverseRate).isGreaterThan(BigDecimal.ONE);
        assertThat(reverseRate).isNotEqualTo(directRate);
    }

    @Test
    void getExchangeRate_WithNullFromCurrency_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate(null, Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getExchangeRate_WithNullToCurrency_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate(Currency.USD, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convert_WithSameCurrency_ReturnsSameAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("1000");

        // Act
        BigDecimal result = exchangeRateService.convert(amount, Currency.USD, Currency.USD);

        // Assert
        assertThat(result).isEqualByComparingTo(amount);
    }

    @Test
    void convert_FromUsdToEur_ReturnsConvertedAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("1000");

        // Act
        BigDecimal result = exchangeRateService.convert(amount, Currency.USD, Currency.EUR);

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("920.00"));
    }

    @Test
    void convert_FromUsdToUah_ReturnsConvertedAmount() {
        // Arrange
        BigDecimal amount = new BigDecimal("100");

        // Act
        BigDecimal result = exchangeRateService.convert(amount, Currency.USD, Currency.UAH);

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("4150.00"));
    }

    @ParameterizedTest
    @CsvSource({
            "USD, EUR, 1000, 920.00",
            "USD, UAH, 100, 4150.00",
            "EUR, UAH, 100, 4500.00",
            "USD, USD, 500, 500.00"
    })
    void convert_WithDifferentCurrencies_ReturnsExpectedAmount(
            Currency from, Currency to, String amountStr, String expectedStr) {
        // Arrange
        BigDecimal amount = new BigDecimal(amountStr);
        BigDecimal expected = new BigDecimal(expectedStr);

        // Act
        BigDecimal result = exchangeRateService.convert(amount, from, to);

        // Assert
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void convert_WithZeroAmount_ReturnsZero() {
        // Arrange
        BigDecimal amount = BigDecimal.ZERO;

        // Act
        BigDecimal result = exchangeRateService.convert(amount, Currency.USD, Currency.EUR);

        // Assert
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void convert_WithNegativeAmount_ThrowsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("-100");

        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.convert(amount, Currency.USD, Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convert_WithNullAmount_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.convert(null, Currency.USD, Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

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

    @Test
    void setExchangeRate_WithNullFromCurrency_ThrowsException() {
        // Arrange
        BigDecimal rate = new BigDecimal("1.00");

        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.setExchangeRate(null, Currency.EUR, rate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setExchangeRate_WithNullToCurrency_ThrowsException() {
        // Arrange
        BigDecimal rate = new BigDecimal("1.00");

        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.setExchangeRate(Currency.USD, null, rate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setExchangeRate_WithNullRate_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.setExchangeRate(Currency.USD, Currency.EUR, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setExchangeRate_WithZeroRate_ThrowsException() {
        // Arrange
        BigDecimal rate = BigDecimal.ZERO;

        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.setExchangeRate(Currency.USD, Currency.EUR, rate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setExchangeRate_WithNegativeRate_ThrowsException() {
        // Arrange
        BigDecimal rate = new BigDecimal("-1.00");

        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.setExchangeRate(Currency.USD, Currency.EUR, rate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clearRates_RemovesAllRates() {
        // Act
        exchangeRateService.clearRates();

        // Assert
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate(Currency.USD, Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convert_AfterClearAndSetRate_UsesNewRate() {
        // Arrange
        exchangeRateService.clearRates();
        exchangeRateService.setExchangeRate(Currency.USD, Currency.EUR, new BigDecimal("1.00"));
        BigDecimal amount = new BigDecimal("100");

        // Act
        BigDecimal result = exchangeRateService.convert(amount, Currency.USD, Currency.EUR);

        // Assert
        assertThat(result).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}