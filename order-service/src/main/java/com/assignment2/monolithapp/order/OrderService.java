package com.assignment2.monolithapp.order;

import com.assignment2.monolithapp.shared.ResourceNotFoundException;
import com.assignment2.monolithapp.order.client.CustomerServiceClient;
import com.assignment2.monolithapp.order.client.ProductServiceClient;
import com.assignment2.monolithapp.order.client.ReservedProduct;
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
    private final CustomerServiceClient customerServiceClient;
    private final ProductServiceClient productServiceClient;

    public OrderService(OrderRepository orderRepository,
                        CustomerServiceClient customerServiceClient,
                        ProductServiceClient productServiceClient) {
        this.orderRepository = orderRepository;
        this.customerServiceClient = customerServiceClient;
        this.productServiceClient = productServiceClient;
    }

    public List<PurchaseOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<PurchaseOrder> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public PurchaseOrder createOrder(PurchaseOrder order) {
        if (!customerServiceClient.customerExists(order.getCustomerId())) {
            throw new ResourceNotFoundException("Customer " + order.getCustomerId() + " was not found");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> normalizedItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            ReservedProduct reservedProduct = productServiceClient.reserveStock(item.getProductId(), item.getQuantity());
            BigDecimal linePrice = reservedProduct.price();
            totalAmount = totalAmount.add(linePrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            normalizedItems.add(new OrderItem(reservedProduct.id(), item.getQuantity(), linePrice));
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
