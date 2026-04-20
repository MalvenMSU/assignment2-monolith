package com.assignment2.monolithapp.order.client;

import java.math.BigDecimal;

public record ReservedProduct(Long id, BigDecimal price, Integer stockAvailable) {
}
