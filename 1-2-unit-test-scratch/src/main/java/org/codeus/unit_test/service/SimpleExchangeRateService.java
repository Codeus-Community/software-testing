package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class SimpleExchangeRateService implements ExchangeRateService {

    private final Map<String, BigDecimal> exchangeRates = new HashMap<>();

    public SimpleExchangeRateService() {
        initializeDefaultRates();
    }

    /**
     * Returns the exchange rate from one currency to another.
     *
     * @param from the source currency
     * @param to the target currency
     * @return the exchange rate as BigDecimal
     * @throws IllegalArgumentException if currencies are null or rate is not available
     */
    @Override
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Currencies cannot be null");
        }

        if (from == to) {
            return BigDecimal.ONE;
        }

        String key = from + "_" + to;
        BigDecimal rate = exchangeRates.get(key);

        if (rate == null) {
            String reverseKey = to + "_" + from;
            BigDecimal reverseRate = exchangeRates.get(reverseKey);

            if (reverseRate != null) {
                rate = BigDecimal.ONE.divide(reverseRate, 6, RoundingMode.HALF_UP);
            } else {
                throw new IllegalArgumentException("Exchange rate not available for " + from + " to " + to);
            }
        }

        return rate;
    }

    @Override
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        BigDecimal rate = getExchangeRate(from, to);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public void setExchangeRate(Currency from, Currency to, BigDecimal rate) {
        if (from == null || to == null || rate == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }

        exchangeRates.put(from + "_" + to, rate);
    }

    public void clearRates() {
        exchangeRates.clear();
    }

    private void initializeDefaultRates() {
        exchangeRates.put("USD_EUR", new BigDecimal("0.92"));
        exchangeRates.put("USD_UAH", new BigDecimal("41.50"));
        exchangeRates.put("EUR_UAH", new BigDecimal("45.00"));
    }
}