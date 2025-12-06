package org.example.shopservice.config;

import org.example.shopservice.model.Product;
import org.example.shopservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            productRepository.saveAll(
                List.of(
                    new Product("Laptop", new BigDecimal("1299.99")),
                    new Product("Headphones", new BigDecimal("199.00")),
                    new Product("Mechanical Keyboard", new BigDecimal("149.50"))
                )
            );
        }
    }
}
