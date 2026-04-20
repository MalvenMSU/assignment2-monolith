package com.assignment2.monolithapp.order;

import com.assignment2.monolithapp.customer.CustomerRepository;
import com.assignment2.monolithapp.product.Product;
import com.assignment2.monolithapp.product.ProductRepository;
import com.assignment2.monolithapp.shared.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    public List<PurchaseOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<PurchaseOrder> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public PurchaseOrder createOrder(PurchaseOrder order) {
        if (!customerRepository.existsById(order.getCustomerId())) {
            throw new ResourceNotFoundException("Customer " + order.getCustomerId() + " was not found");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> normalizedItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product " + item.getProductId() + " was not found"));

            if (product.getStockAvailable() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Product " + product.getId() + " has only " + product.getStockAvailable() + " items remaining"
                );
            }

            product.setStockAvailable(product.getStockAvailable() - item.getQuantity());
            productRepository.save(product);

            BigDecimal linePrice = product.getPrice();
            totalAmount = totalAmount.add(linePrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            normalizedItems.add(new OrderItem(product.getId(), item.getQuantity(), linePrice));
        }

        order.setItems(normalizedItems);
        order.setTotalAmount(totalAmount);

        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }

        if (order.getStatus() == null || order.getStatus().isBlank()) {
            order.setStatus("CREATED");
        }

        PurchaseOrder savedOrder = orderRepository.save(order);
        log.info("Created order id={} for customer={} with {} items and total={}",
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getItems().size(),
                savedOrder.getTotalAmount());
        return savedOrder;
    }
}
