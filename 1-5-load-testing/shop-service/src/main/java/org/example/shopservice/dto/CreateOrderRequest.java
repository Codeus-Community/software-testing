package org.example.shopservice.dto;

public record CreateOrderRequest(Long productId, Integer quantity) {
}
