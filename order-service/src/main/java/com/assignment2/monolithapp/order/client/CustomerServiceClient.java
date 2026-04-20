package com.assignment2.monolithapp.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CustomerServiceClient {

    private final RestClient restClient;

    public CustomerServiceClient(RestClient.Builder builder,
                                 @Value("${services.customer.base-url}") String customerServiceBaseUrl) {
        this.restClient = builder.baseUrl(customerServiceBaseUrl).build();
    }

    public boolean customerExists(Long id) {
        try {
            restClient.get()
                    .uri("/api/customers/{id}", id)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {})
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return false;
            }
            throw ex;
        }
    }
}
