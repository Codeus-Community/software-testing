package org.codeus.integration_test.controller;

import org.codeus.integration_test.dto.response.RateResponse;
import org.codeus.integration_test.mapper.RateMapper;
import org.codeus.integration_test.service.RateService;
import org.codeus.integration_test.validation.ValidCurrencyCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rates")
@Validated
@RequiredArgsConstructor
public class RateController {

    private final RateService rateService;
    private final RateMapper rateMapper;

    @GetMapping("/{base}/{target}")
    public ResponseEntity<RateResponse> getExchangeRate(
            @ValidCurrencyCode @PathVariable String base,
            @ValidCurrencyCode @PathVariable String target) {
        RateResponse response = rateMapper.toResponse(base, target, rateService.getExchangeRate(base, target), LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{base}")
    public ResponseEntity<Map<String, BigDecimal>> getAllRatesForBase(
            @ValidCurrencyCode @PathVariable String base) {
        Map<String, BigDecimal> rates = rateService.getAllRatesForBase(base);
        return ResponseEntity.ok(rates);
    }
}
