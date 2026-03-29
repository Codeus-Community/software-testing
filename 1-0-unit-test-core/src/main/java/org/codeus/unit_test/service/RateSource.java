package org.codeus.unit_test.service;

import java.math.BigDecimal;
import java.util.Map;

public interface RateSource {
    Map<String, BigDecimal> loadRates();
}