package org.codeus.integration_test.mapper;

import org.codeus.integration_test.dto.response.ExchangeResponse;
import org.codeus.integration_test.model.Exchange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExchangeMapper {

    @Mapping(target = "status", expression = "java(exchange.getStatus().name())")
    ExchangeResponse toResponse(Exchange exchange);

    List<ExchangeResponse> toResponseList(List<Exchange> exchanges);
}
