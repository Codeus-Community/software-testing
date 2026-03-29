package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class OnlineExchangeRateService extends SimpleExchangeRateService {

    public OnlineExchangeRateService(RateSource rateSource) {
        super(rateSource);
    }

    @Override
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        BigDecimal exchangeRate = super.getExchangeRate(from, to);
        makeDelay();
        return exchangeRate;
    }

    @Override
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        BigDecimal converted = super.convert(amount, from, to);
        makeDelay();
        return converted;
    }

    private void makeDelay() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Exchange rate request was interrupted", e);
        }
    }
}
