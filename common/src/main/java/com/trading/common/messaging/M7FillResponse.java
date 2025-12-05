package com.trading.common.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Execution result from Mock M7 - order was filled or rejected.
 *
 * <p>This message travels through RabbitMQ: Mock M7 → m7.topic → Order Service
 *
 * <p>M7 sends this after a delay (500ms - 2s) simulating market execution. The order is either
 * filled (executed) or rejected.
 *
 * @param correlationId Same ID from the original order
 * @param orderId The executed/rejected order
 * @param filled True if order was filled, false if rejected
 * @param executionPrice The price at which the order was filled (null if rejected)
 * @param rejectReason Reason for rejection (null if filled)
 * @param timestamp When execution occurred
 */
public record M7FillResponse(
    String correlationId,
    String orderId,
    boolean filled,
    BigDecimal executionPrice,
    String rejectReason,
    Instant timestamp) {

  public M7FillResponse {
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

  /** Factory method for a successful fill. */
  public static M7FillResponse filled(
      String correlationId, String orderId, BigDecimal executionPrice) {
    return new M7FillResponse(correlationId, orderId, true, executionPrice, null, Instant.now());
  }

  /** Factory method for a rejection. */
  public static M7FillResponse rejected(String correlationId, String orderId, String reason) {
    return new M7FillResponse(correlationId, orderId, false, null, reason, Instant.now());
  }
}
