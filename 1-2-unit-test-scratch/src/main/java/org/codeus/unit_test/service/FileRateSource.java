package org.codeus.unit_test.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads exchange rates from a text file.
 *
 * File format: each line should contain "FROM_TO=rate"
 * Example:
 * USD_EUR=0.92
 * USD_UAH=41.50
 */
public class FileRateSource implements RateSource {

    private final String filePath;

    public FileRateSource(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Map<String, BigDecimal> loadRates() {
        Map<String, BigDecimal> rates = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String currencyPair = parts[0].trim();
                    BigDecimal rate = new BigDecimal(parts[1].trim());
                    rates.put(currencyPair, rate);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load exchange rates from file: " + filePath, e);
        }

        return rates;
    }
}