package com.trading.common.messaging;

import com.trading.common.Region;
import java.math.BigDecimal;

/**
 * Message sent from Gateway to Order Service when a user submits an order.
 *
 * <p>This message travels through RabbitMQ: Gateway → orders.topic → Order Service
 *
 * @param correlationId Unique ID to track this order across all services
 * @param orderId Unique order identifier (UUID)
 * @param username The authenticated user who placed the order
 * @param region Trading region - must be one of NORTH, SOUTH, EAST, WEST
 * @param orderType "BUY" or "SELL"
 * @param quantity Amount of electricity (MWh)
 * @param price Price per MWh
 */
public record OrderSubmitMessage(
    String correlationId,
    String orderId,
    String username,
    Region region,
    String orderType,
    BigDecimal quantity,
    BigDecimal price) {

  public OrderSubmitMessage {
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalArgumentException("correlationId cannot be null or blank");
    }
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("orderId cannot be null or blank");
    }
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("username cannot be null or blank");
    }
  }
}
