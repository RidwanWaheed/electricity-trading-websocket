package com.trading.common.messaging;

import com.trading.common.Region;
import java.math.BigDecimal;

/**
 * Message sent from Order Service to Mock M7 (trading engine).
 *
 * <p>This message travels through RabbitMQ: Order Service → m7.topic → Mock M7
 *
 * @param correlationId Same ID from the original order - preserved through entire flow
 * @param orderId The order ID
 * @param region Trading region
 * @param orderType "BUY" or "SELL"
 * @param quantity Amount of electricity (MWh)
 * @param price Price per MWh
 */
public record M7OrderRequest(
    String correlationId,
    String orderId,
    Region region,
    String orderType,
    BigDecimal quantity,
    BigDecimal price) {

  public M7OrderRequest {
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalArgumentException("correlationId cannot be null or blank");
    }
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("orderId cannot be null or blank");
    }
  }
}
