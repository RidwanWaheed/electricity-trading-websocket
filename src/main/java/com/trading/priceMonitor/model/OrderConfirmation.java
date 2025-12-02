package com.trading.priceMonitor.model;

import java.time.Instant;

public record OrderConfirmation(
    String orderId,
    Status status,
    String message,
    Instant confirmedAt) {

  public OrderConfirmation {
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("Order ID cannot be null or blank");
    }
    if (status == null) {
      throw new IllegalArgumentException("Status cannot be null");
    }
    if (confirmedAt == null) {
      throw new IllegalArgumentException("ConfirmedAt cannot be null");
    }
  }
}
