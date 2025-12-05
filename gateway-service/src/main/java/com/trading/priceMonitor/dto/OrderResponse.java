package com.trading.priceMonitor.dto;

import com.trading.priceMonitor.entity.OrderEntity;
import com.trading.priceMonitor.model.Status;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    String orderId,
    String region,
    String orderType,
    BigDecimal quantity,
    BigDecimal price,
    Status status,
    Instant createdAt) {
  public static OrderResponse from(OrderEntity entity) {
    return new OrderResponse(
        entity.getOrderId(),
        entity.getRegion(),
        entity.getOrderType(),
        entity.getQuantity(),
        entity.getPrice(),
        entity.getStatus(),
        entity.getCreatedAt());
  }
}
