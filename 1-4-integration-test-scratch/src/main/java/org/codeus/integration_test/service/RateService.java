package org.codeus.integration_test.service;

import org.codeus.integration_test.client.ExchangeRateApiClient;
import org.codeus.integration_test.client.dto.ExchangeRateApiResponse;
import org.codeus.integration_test.exception.ExchangeRateNotFoundException;
import org.codeus.integration_test.util.CurrencyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateService {

    private static final String RATES_CACHE = "rates";
    private static final String RATES_ALL_CACHE = "rates-all";

    private final ExchangeRateApiClient apiClient;


    @Cacheable(value = RATES_CACHE, key = "#baseCurrency + ':' + #targetCurrency")
    public BigDecimal getExchangeRate(String baseCurrency, String targetCurrency) {
        final String normalizedBase = CurrencyUtils.normalizeCurrencyCode(baseCurrency);
        final String normalizedTarget = CurrencyUtils.normalizeCurrencyCode(targetCurrency);
        log.info("Getting exchange rate from {} to {}", normalizedBase, normalizedTarget);

        if (normalizedBase.equals(normalizedTarget)) {
            return BigDecimal.ONE;
        }

        ExchangeRateApiResponse response = apiClient.fetchExchangeRates(baseCurrency);
        BigDecimal rate = response.getRates().get(targetCurrency);

        if (rate == null) {
            throw new ExchangeRateNotFoundException("Rate not found for " + targetCurrency + " in response");
        }
        return rate;
    }

    @Cacheable(value = RATES_ALL_CACHE, key = "#baseCurrency")
    public Map<String, BigDecimal> getAllRatesForBase(String baseCurrency) {
        final String normalizedBase = CurrencyUtils.normalizeCurrencyCode(baseCurrency);
        log.info("Getting all exchange rates for base currency: {}", normalizedBase);
        return apiClient.fetchExchangeRates(baseCurrency).getRates();
    }

    @CacheEvict(value = {RATES_CACHE, RATES_ALL_CACHE}, allEntries = true)
    public void clearCache() {
        log.info("Clearing exchange rate cache");
    }


}
