package org.codeus.integration_test.controller;

import org.codeus.integration_test.dto.request.ExchangeRequest;
import org.codeus.integration_test.dto.response.ExchangeResponse;
import org.codeus.integration_test.mapper.ExchangeMapper;
import org.codeus.integration_test.model.Exchange;
import org.codeus.integration_test.service.ExchangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final ExchangeMapper exchangeMapper;

    @PostMapping
    public ResponseEntity<ExchangeResponse> performExchange(@Valid @RequestBody ExchangeRequest request) {
        Exchange exchange = exchangeService.performExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exchangeMapper.toResponse(exchange));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExchangeResponse> getExchangeById(@PathVariable Long id) {
        Exchange exchange = exchangeService.getExchangeById(id);
        ExchangeResponse response = exchangeMapper.toResponse(exchange);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExchangeResponse>> getUserExchanges(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        List<Exchange> exchanges = exchangeService.getUserExchanges(userId, page, size);
        List<ExchangeResponse> responses = exchangeMapper.toResponseList(exchanges);

        return ResponseEntity.ok(responses);
    }
}
