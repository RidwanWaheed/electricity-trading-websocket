package com.trading.common.messaging;

import com.trading.common.OrderStatus;
import java.time.Instant;

/**
 * Message sent from Order Service to Gateway when an order's status changes.
 *
 * <p>This message travels through RabbitMQ: Order Service → orders.topic → Gateway The Gateway then
 * pushes this to the specific user via WebSocket.
 *
 * @param correlationId Same ID from the original OrderSubmitMessage
 * @param orderId The order that changed status
 * @param username The user to notify (used for routing to correct WebSocket session)
 * @param status New status of the order
 * @param message Human-readable status message (e.g., "Order filled at 45.50 EUR/MWh")
 * @param timestamp When the status change occurred
 */
public record OrderStatusMessage(
    String correlationId,
    String orderId,
    String username,
    OrderStatus status,
    String message,
    Instant timestamp) {

  public OrderStatusMessage {
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalArgumentException("correlationId cannot be null or blank");
    }
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("orderId cannot be null or blank");
    }
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("username cannot be null or blank");
    }
    if (status == null) {
      throw new IllegalArgumentException("status cannot be null");
    }
    if (timestamp == null) {
      timestamp = Instant.now();
    }
  }
}
