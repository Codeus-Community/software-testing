package org.example.shopservice.service;

import org.example.shopservice.dto.CreateOrderRequest;
import org.example.shopservice.dto.OrderResponse;
import org.example.shopservice.model.Product;
import org.example.shopservice.model.ShopOrder;
import org.example.shopservice.repository.ShopOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final ShopOrderRepository orderRepository;
    private final ProductService productService;

    public OrderService(ShopOrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request == null || request.productId() == null || request.quantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and quantity are required");
        }
        if (request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }

        Product product = productService.requireProduct(request.productId());
        ShopOrder order = orderRepository.save(new ShopOrder(product, request.quantity()));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        ShopOrder order = orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return OrderResponse.from(order);
    }
}
