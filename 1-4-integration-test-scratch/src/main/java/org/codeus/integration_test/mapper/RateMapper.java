package org.codeus.integration_test.mapper;

import org.codeus.integration_test.dto.response.RateResponse;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface RateMapper {


    default RateResponse toResponse(String baseCurrency, String targetCurrency,
                                    BigDecimal rate,
                                    LocalDateTime timestamp) {
        return RateResponse.builder()
                .baseCurrency(baseCurrency)
                .targetCurrency(targetCurrency)
                .rate(rate)
                .timestamp(timestamp)
                .build();
    }
}
