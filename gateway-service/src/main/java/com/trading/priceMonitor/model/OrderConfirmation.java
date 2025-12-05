package com.trading.priceMonitor.model;

import com.trading.common.OrderStatus;
import java.time.Instant;

public record OrderConfirmation(
    String orderId, OrderStatus status, String message, Instant confirmedAt) {

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
