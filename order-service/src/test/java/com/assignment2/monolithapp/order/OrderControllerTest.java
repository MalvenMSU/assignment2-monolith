package com.assignment2.monolithapp.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.assignment2.monolithapp.order.client.CustomerServiceClient;
import com.assignment2.monolithapp.order.client.ProductServiceClient;
import com.assignment2.monolithapp.order.client.ReservedProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerServiceClient customerServiceClient;

    @MockitoBean
    private ProductServiceClient productServiceClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void shouldCreateOrder() throws Exception {
        Mockito.when(customerServiceClient.customerExists(1L)).thenReturn(true);
        Mockito.when(productServiceClient.reserveStock(100L, 1))
                .thenReturn(new ReservedProduct(100L, new BigDecimal("1500.00"), 9));

        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerId(1L);
        order.setItems(List.of(new OrderItem(100L, 1, null)));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(1L))
                .andExpect(jsonPath("$.totalAmount").value(1500.00))
                .andExpect(jsonPath("$.items[0].productId").value(100L));
    }

    @Test
    void shouldGetOrders() throws Exception {
        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerId(2L);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setItems(List.of(new OrderItem(200L, 1, new BigDecimal("50.00"))));
        orderRepository.save(order);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value(2L));
    }

    @Test
    void shouldRejectOrderWhenProductStockIsInsufficient() throws Exception {
        Mockito.when(customerServiceClient.customerExists(1L)).thenReturn(true);
        Mockito.when(productServiceClient.reserveStock(300L, 2))
                .thenThrow(new InsufficientStockException("Product 300 could not be reserved"));

        PurchaseOrder order = new PurchaseOrder();
        order.setCustomerId(1L);
        order.setItems(List.of(new OrderItem(300L, 2, null)));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
