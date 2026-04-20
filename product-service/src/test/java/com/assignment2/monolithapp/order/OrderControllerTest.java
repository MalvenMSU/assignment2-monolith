package com.assignment2.monolithapp.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.assignment2.monolithapp.customer.Customer;
import com.assignment2.monolithapp.customer.CustomerRepository;
import com.assignment2.monolithapp.product.Product;
import com.assignment2.monolithapp.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        customerRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void shouldCreateOrder() throws Exception {
        Customer customer = customerRepository.save(new Customer("John", "Doe", "john.doe@example.com"));
        Product product = productRepository.save(new Product("Laptop", "Gaming laptop", new BigDecimal("1500.00"), 10));

        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerId(customer.getId());
        order.setItems(List.of(new OrderItem(product.getId(), 1, null)));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.totalAmount").value(1500.00))
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()));
    }

    @Test
    void shouldGetOrders() throws Exception {
        Customer customer = customerRepository.save(new Customer("Jane", "Doe", "jane.doe@example.com"));
        Product product = productRepository.save(new Product("Keyboard", "Mechanical keyboard", new BigDecimal("50.00"), 5));
        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerId(customer.getId());
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setItems(List.of(new OrderItem(product.getId(), 1, new BigDecimal("50.00"))));
        orderRepository.save(order);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value(customer.getId()));
    }

    @Test
    void shouldRejectOrderWhenProductStockIsInsufficient() throws Exception {
        Customer customer = customerRepository.save(new Customer("John", "Doe", "john.doe@example.com"));
        Product product = productRepository.save(new Product("Mouse", "Wireless mouse", new BigDecimal("50.00"), 1));

        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerId(customer.getId());
        order.setItems(List.of(new OrderItem(product.getId(), 2, null)));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
