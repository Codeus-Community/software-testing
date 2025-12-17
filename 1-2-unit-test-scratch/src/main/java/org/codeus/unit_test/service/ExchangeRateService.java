package org.codeus.unit_test.service;

import org.codeus.unit_test.enums.Currency;

import java.math.BigDecimal;

public interface ExchangeRateService {
    BigDecimal getExchangeRate(Currency from, Currency to);
    BigDecimal convert(BigDecimal amount, Currency from, Currency to);
}