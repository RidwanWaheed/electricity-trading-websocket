package com.trading.priceMonitor.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ElectricityPrice(
    String area,
    String region,
    BigDecimal price,
    String currency,
    double changePercent,
    Instant timestamp) {
  public ElectricityPrice {
    if (area == null || area.isBlank()) {
      throw new IllegalArgumentException("Area cannot be null or blank");
    }
    if (region == null || region.isBlank()) {
      throw new IllegalArgumentException("Region cannot be null or blank");
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
