package com.trading.common.messaging;

import java.time.Instant;

/**
 * Immediate acknowledgment from Mock M7 that an order was received.
 *
 * <p>This message travels through RabbitMQ: Mock M7 → m7.topic → Order Service
 *
 * <p>M7 sends this immediately (< 50ms) after receiving an order request. This confirms the trading
 * engine received the order, but doesn't mean it's been executed yet.
 *
 * @param correlationId Same ID from the original order
 * @param orderId The acknowledged order
 * @param m7ReferenceId Internal reference ID assigned by M7
 * @param timestamp When M7 acknowledged the order
 */
public record M7AckResponse(
    String correlationId, String orderId, String m7ReferenceId, Instant timestamp) {

  public M7AckResponse {
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalArgumentException("correlationId cannot be null or blank");
    }
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("orderId cannot be null or blank");
    }
    if (timestamp == null) {
      timestamp = Instant.now();
    }
  }
}
