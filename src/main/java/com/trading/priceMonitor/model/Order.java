package com.trading.priceMonitor.model;

import java.math.BigDecimal;

public record Order(
    String orderId,
    String region,
    BigDecimal price,
    String type,
    BigDecimal quantity) {
  public Order {
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("Order ID cannot be null or blank");
    }
    if (region == null || region.isBlank()) {
      throw new IllegalArgumentException("Region cannot be null or blank");
    }
    if (price == null) {
      throw new IllegalArgumentException("Price cannot be null");
    }
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Type cannot be null or blank");
    }
    if (quantity == null) {
      throw new IllegalArgumentException("Quantity cannot be null");
    }
  }
}
