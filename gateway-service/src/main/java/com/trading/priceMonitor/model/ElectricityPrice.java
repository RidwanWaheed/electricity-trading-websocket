package com.trading.priceMonitor.model;

import com.trading.common.Region;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Real-time electricity price broadcast to WebSocket clients.
 *
 * <p>Sent to all subscribers on /topic/prices. Originates from Mock M7 (simulating EPEX SPOT
 * exchange), passes through RabbitMQ, and is broadcast by Gateway to connected clients.
 *
 * @param area Market area (e.g., "Germany")
 * @param region Trading region (type-safe enum: NORTH, SOUTH, EAST, WEST)
 * @param price Current price in the specified currency per MWh
 * @param currency Price currency (e.g., "EUR")
 * @param changePercent Price change from previous update (e.g., 2.5 for +2.5%)
 * @param timestamp When this price was generated (UTC)
 */
public record ElectricityPrice(
    String area,
    Region region,
    BigDecimal price,
    String currency,
    double changePercent,
    Instant timestamp) {

  public ElectricityPrice {
    if (area == null || area.isBlank()) {
      throw new IllegalArgumentException("Area cannot be null or blank");
    }
    if (price == null) {
      throw new IllegalArgumentException("Price cannot be null");
    }
    if (currency == null || currency.isBlank()) {
      throw new IllegalArgumentException("Currency cannot be null or blank");
    }
    if (timestamp == null) {
      throw new IllegalArgumentException("Timestamp cannot be null");
    }
  }
}
