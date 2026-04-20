package com.assignment2.monolithapp.order.client;

import com.assignment2.monolithapp.order.InsufficientStockException;
import com.assignment2.monolithapp.shared.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductServiceClient {

    private final RestClient restClient;

    public ProductServiceClient(RestClient.Builder builder,
                                @Value("${services.product.base-url}") String productServiceBaseUrl) {
        this.restClient = builder.baseUrl(productServiceBaseUrl).build();
    }

    public ReservedProduct reserveStock(Long productId, Integer quantity) {
        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/api/products/{id}/reserve")
                            .queryParam("quantity", quantity)
                            .build(productId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {})
                    .body(ReservedProduct.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Product " + productId + " was not found");
            }

            if (ex.getStatusCode().value() == 400) {
                throw new InsufficientStockException("Product " + productId + " could not be reserved");
            }

            throw ex;
        }
    }
}
