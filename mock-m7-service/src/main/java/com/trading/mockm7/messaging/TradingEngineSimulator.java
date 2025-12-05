package com.trading.mockm7.messaging;

import com.trading.common.messaging.M7AckResponse;
import com.trading.common.messaging.M7FillResponse;
import com.trading.common.messaging.M7OrderRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simulates realistic trading engine behavior.
 *
 * <p>In a real M7/EPEX SPOT system:
 *
 * <ul>
 *   <li>Orders are matched against the order book
 *   <li>ACK is sent immediately when order enters the book
 *   <li>FILL is sent when a matching counterparty is found
 *   <li>Execution price may differ from requested price (price improvement or slippage)
 * </ul>
 *
 * <p>Our simulation:
 *
 * <ul>
 *   <li>ACK sent immediately (< 50ms)
 *   <li>FILL sent after random delay (500ms - 2000ms)
 *   <li>90% fill rate, 10% rejection rate
 *   <li>Execution price varies ±2% from requested price
 * </ul>
 */
@Component
public class TradingEngineSimulator {

  private static final Logger log = LoggerFactory.getLogger(TradingEngineSimulator.class);

  /** Probability of order being filled (0.0 to 1.0) */
  private static final double FILL_RATE = 0.90;

  /** Minimum delay before fill response (milliseconds) */
  private static final int MIN_FILL_DELAY_MS = 500;

  /** Maximum delay before fill response (milliseconds) */
  private static final int MAX_FILL_DELAY_MS = 2000;

  /** Maximum price variation from requested price (percentage) */
  private static final double MAX_PRICE_VARIATION_PERCENT = 0.02;

  private final Random random = new Random();
  private final M7ResponsePublisher responsePublisher;

  public TradingEngineSimulator(M7ResponsePublisher responsePublisher) {
    this.responsePublisher = responsePublisher;
  }

  /**
   * Process an incoming order request.
   *
   * <p>This simulates the two-phase response pattern:
   *
   * <ol>
   *   <li>Immediate ACK - confirms order received
   *   <li>Delayed FILL - confirms order executed (or rejected)
   * </ol>
   */
  public void processOrder(M7OrderRequest order) {
    log.info(
        "[corr-id={}] Processing order: orderId={}, type={}, qty={}, price={}",
        order.correlationId(),
        order.orderId(),
        order.orderType(),
        order.quantity(),
        order.price());

    // Phase 1: Immediate ACK
    sendAcknowledgment(order);

    // Phase 2: Delayed FILL (async)
    scheduleExecution(order);
  }

  /**
   * Send immediate acknowledgment.
   *
   * <p>This confirms the order was received and is in the order book. The M7 reference ID is our
   * internal tracking number.
   */
  private void sendAcknowledgment(M7OrderRequest order) {
    String m7ReferenceId = generateM7ReferenceId();

    M7AckResponse ack =
        new M7AckResponse(order.correlationId(), order.orderId(), m7ReferenceId, Instant.now());

    log.info("[corr-id={}] Order acknowledged, m7RefId={}", order.correlationId(), m7ReferenceId);

    responsePublisher.publishAck(ack);
  }

  /**
   * Schedule delayed order execution.
   *
   * <p>Uses CompletableFuture to avoid blocking the RabbitMQ listener thread. The delay simulates
   * the time it takes to find a matching counterparty in the market.
   */
  private void scheduleExecution(M7OrderRequest order) {
    int delayMs = randomDelay();

    log.debug("[corr-id={}] Scheduling execution in {}ms", order.correlationId(), delayMs);

    CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)
        .execute(() -> executeOrder(order));
  }

  /**
   * Execute the order (fill or reject).
   *
   * <p>This runs after the simulated delay and determines whether the order is filled or rejected.
   */
  private void executeOrder(M7OrderRequest order) {
    boolean shouldFill = random.nextDouble() < FILL_RATE;

    if (shouldFill) {
      BigDecimal executionPrice = calculateExecutionPrice(order.price());

      log.info(
          "[corr-id={}] Order FILLED at {} (requested: {})",
          order.correlationId(),
          executionPrice,
          order.price());

      responsePublisher.publishFill(
          M7FillResponse.filled(order.correlationId(), order.orderId(), executionPrice));
    } else {
      String reason = generateRejectReason();

      log.info("[corr-id={}] Order REJECTED: {}", order.correlationId(), reason);

      responsePublisher.publishFill(
          M7FillResponse.rejected(order.correlationId(), order.orderId(), reason));
    }
  }

  /**
   * Generate an M7 reference ID.
   *
   * <p>In real M7, this would be assigned by the exchange. We simulate it with a UUID prefix.
   */
  private String generateM7ReferenceId() {
    return "M7-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  /**
   * Calculate random delay for order execution.
   *
   * @return Delay in milliseconds between MIN and MAX
   */
  private int randomDelay() {
    return MIN_FILL_DELAY_MS + random.nextInt(MAX_FILL_DELAY_MS - MIN_FILL_DELAY_MS);
  }

  /**
   * Calculate execution price with small variation.
   *
   * <p>In real markets, execution price can differ from requested price due to:
   *
   * <ul>
   *   <li>Price improvement (better price found)
   *   <li>Slippage (market moved against you)
   * </ul>
   */
  private BigDecimal calculateExecutionPrice(BigDecimal requestedPrice) {
    // Random variation between -2% and +2%
    double variation = (random.nextDouble() * 2 - 1) * MAX_PRICE_VARIATION_PERCENT;
    BigDecimal multiplier = BigDecimal.valueOf(1 + variation);

    return requestedPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Generate a random rejection reason.
   *
   * <p>Real rejections happen due to insufficient liquidity, price limits, regulatory constraints,
   * etc.
   */
  private String generateRejectReason() {
    String[] reasons = {
      "Insufficient market liquidity",
      "Price outside acceptable range",
      "Market temporarily closed",
      "Order size exceeds limit",
      "Counterparty not available"
    };
    return reasons[random.nextInt(reasons.length)];
  }
}
