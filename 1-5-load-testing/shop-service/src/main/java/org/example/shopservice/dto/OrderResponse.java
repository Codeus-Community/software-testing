package org.example.shopservice.dto;

import org.example.shopservice.model.ShopOrder;

import java.time.Instant;

public record OrderResponse(Long id, Long productId, int quantity, Instant createdAt) {

    public static OrderResponse from(ShopOrder order) {
        return new OrderResponse(
            order.getId(),
            order.getProduct().getId(),
            order.getQuantity(),
            order.getCreatedAt()
        );
    }
}
