package com.trading.mockm7.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.Region;
import com.trading.common.messaging.PriceUpdate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Publishes simulated price updates to Gateway.
 *
 * <p>In real trading systems, price data ("market data") flows from the exchange to all connected
 * participants. This is different from order flow:
 *
 * <ul>
 *   <li><b>Order flow</b>: Request/Response (your order → exchange → confirmation back to you)
 *   <li><b>Market data</b>: Pub/Sub broadcast (exchange → all subscribers)
 * </ul>
 *
 * <p>Real exchanges like EPEX SPOT publish price updates at high frequency. We simulate this with a
 * 2-second interval for demonstration purposes.
 *
 * <p>Message flow: Mock M7 → prices.topic (price.{region}) → Gateway → WebSocket → Browser
 */
@Component
public class PricePublisher {

  private static final Logger log = LoggerFactory.getLogger(PricePublisher.class);

  private static final String CURRENCY = "EUR";

  /** Base price range for simulation (EUR/MWh) */
  private static final double MIN_PRICE = 50.0;

  private static final double MAX_PRICE = 150.0;

  /** Maximum price change between updates (percentage) */
  private static final double MAX_CHANGE_PERCENT = 5.0;

  private final RabbitTemplate rabbitTemplate;
  private final Random random = new Random();

  /**
   * Tracks previous price per region to calculate deltas.
   *
   * <p><b>Why EnumMap instead of HashMap:</b> EnumMap is optimized for enum keys - it uses an array
   * internally where the index is the enum's ordinal value. This makes it:
   *
   * <ul>
   *   <li>Faster than HashMap (O(1) with no hashing overhead)
   *   <li>More memory efficient (no Entry objects needed)
   *   <li>Iteration order matches enum declaration order
   * </ul>
   */
  private final Map<Region, BigDecimal> previousPrices = new EnumMap<>(Region.class);

  public PricePublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
    // Initialize with random starting prices for all regions
    for (Region region : Region.values()) {
      previousPrices.put(region, generateRandomPrice());
    }
  }

  /**
   * Broadcast price updates for all regions.
   *
   * <p>Runs every 2 seconds. In production, this would be triggered by actual market events.
   */
  @Scheduled(fixedRate = 2000)
  public void publishPrices() {
    for (Region region : Region.values()) {
      PriceUpdate update = generatePriceUpdate(region);
      String routingKey = String.format(ROUTING_PRICE_PATTERN, region.name());

      rabbitTemplate.convertAndSend(PRICES_EXCHANGE, routingKey, update);

      log.debug(
          "Published price for {}: {} {} ({}%)",
          region, update.price(), update.currency(), String.format("%.2f", update.changePercent()));
    }
  }

  /**
   * Generate a price update with realistic delta from previous price.
   *
   * <p>Real markets don't jump randomly - prices move incrementally. We simulate this by limiting
   * the change from the previous price.
   */
  private PriceUpdate generatePriceUpdate(Region region) {
    BigDecimal previousPrice = previousPrices.get(region);

    // Calculate new price with small random change (±5% max)
    double changePercent = (random.nextDouble() * 2 - 1) * MAX_CHANGE_PERCENT;
    BigDecimal multiplier = BigDecimal.valueOf(1 + changePercent / 100);
    BigDecimal newPrice = previousPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

    // Keep price within bounds
    if (newPrice.doubleValue() < MIN_PRICE) {
      newPrice = BigDecimal.valueOf(MIN_PRICE);
    } else if (newPrice.doubleValue() > MAX_PRICE) {
      newPrice = BigDecimal.valueOf(MAX_PRICE);
    }

    // Calculate actual change percent
    double actualChange = 0.0;
    if (previousPrice.compareTo(BigDecimal.ZERO) != 0) {
      actualChange =
          newPrice
              .subtract(previousPrice)
              .divide(previousPrice, 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .doubleValue();
    }

    // Update previous price for next iteration
    previousPrices.put(region, newPrice);

    return new PriceUpdate(region, newPrice, CURRENCY, actualChange, Instant.now());
  }

  /** Generate a random starting price within the configured range. */
  private BigDecimal generateRandomPrice() {
    double price = MIN_PRICE + (MAX_PRICE - MIN_PRICE) * random.nextDouble();
    return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
  }
}
