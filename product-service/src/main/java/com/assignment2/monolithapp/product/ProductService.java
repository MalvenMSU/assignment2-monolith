package com.assignment2.monolithapp.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        log.info("Created product with id={} and stock={}", savedProduct.getId(), savedProduct.getStockAvailable());
        return savedProduct;
    }

    @Transactional
    public Product reserveStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new com.assignment2.monolithapp.shared.ResourceNotFoundException("Product " + id + " was not found"));

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (product.getStockAvailable() < quantity) {
            throw new InsufficientStockException(
                    "Product " + product.getId() + " has only " + product.getStockAvailable() + " items remaining"
            );
        }

        product.setStockAvailable(product.getStockAvailable() - quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Reserved {} unit(s) of product id={}, remaining stock={}",
                quantity,
                updatedProduct.getId(),
                updatedProduct.getStockAvailable());
        return updatedProduct;
    }
}
