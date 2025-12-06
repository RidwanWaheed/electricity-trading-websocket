package com.trading.priceMonitor.model;

import com.trading.common.OrderStatus;
import java.time.Instant;

/**
 * Order status update sent to a specific user via WebSocket.
 *
 * <p>Sent to the user's personal queue at /user/queue/order-confirmation. Tracks the order through
 * its lifecycle: PENDING → SUBMITTED → FILLED/REJECTED.
 *
 * @param orderId The order this confirmation relates to
 * @param status Current status (PENDING, SUBMITTED, FILLED, REJECTED)
 * @param message Human-readable status message (e.g., "Order filled at 45.50 EUR/MWh")
 * @param confirmedAt When this status change occurred (UTC)
 */
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
