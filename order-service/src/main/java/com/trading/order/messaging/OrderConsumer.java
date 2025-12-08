package com.trading.order.messaging;

import static com.trading.common.messaging.RabbitMQConstants.QUEUE_ORDER_SUBMISSIONS;

import com.trading.common.OrderStatus;
import com.trading.common.messaging.OrderSubmitMessage;
import com.trading.order.entity.OrderEntity;
import com.trading.order.repository.OrderRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes order submissions from Gateway.
 *
 * <p>Message flow: Gateway → orders.topic (order.submit) → Order Service
 *
 * <p>When an order arrives:
 *
 * <ol>
 *   <li>Validate the order
 *   <li>Save to database with status PENDING
 *   <li>Notify Gateway of PENDING status
 *   <li>Forward to Mock M7 for execution
 * </ol>
 */
@Component
public class OrderConsumer {

  private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

  private final OrderRepository orderRepository;
  private final M7Publisher m7Publisher;
  private final StatusPublisher statusPublisher;

  public OrderConsumer(
      OrderRepository orderRepository, M7Publisher m7Publisher, StatusPublisher statusPublisher) {
    this.orderRepository = orderRepository;
    this.m7Publisher = m7Publisher;
    this.statusPublisher = statusPublisher;
  }

  /**
   * Process an order submission from Gateway.
   *
   * <p>This is where the order lifecycle begins in Order Service.
   */
  @RabbitListener(queues = QUEUE_ORDER_SUBMISSIONS)
  public void onOrderSubmit(OrderSubmitMessage message) {
    log.info(
        "[corr-id={}] Received order from Gateway: orderId={}, user={}",
        message.correlationId(),
        message.orderId(),
        message.username());

    if (!isValidOrder(message)) {
      log.warn("[corr-id={}] Order validation failed", message.correlationId());
      statusPublisher.publishStatusUpdate(
          message.correlationId(),
          message.orderId(),
          message.username(),
          OrderStatus.REJECTED,
          "Order validation failed");
      return;
    }

    OrderEntity order =
        new OrderEntity(
            message.orderId(),
            message.correlationId(),
            message.username(),
            message.region(),
            message.orderType(),
            message.quantity(),
            message.price());

    orderRepository.save(order);
    log.info("[corr-id={}] Order saved with status PENDING", message.correlationId());

    m7Publisher.publishOrder(order);
    log.info("[corr-id={}] Order forwarded to M7", message.correlationId());
  }

  /**
   * Validate the order including trading limits.
   *
   * <p>Validates:
   *
   * <ul>
   *   <li>Valid region
   *   <li>Price within acceptable range (0.01 - 500 EUR/MWh)
   *   <li>Quantity within limits (0.1 - 1000 MWh)
   *   <li>Valid order type (BUY/SELL)
   * </ul>
   */
  private boolean isValidOrder(OrderSubmitMessage message) {
    if (message.quantity() == null || message.quantity().signum() <= 0) {
      log.warn("[corr-id={}] Invalid quantity: {}", message.correlationId(), message.quantity());
      return false;
    }
    if (message.price() == null || message.price().signum() <= 0) {
      log.warn("[corr-id={}] Invalid price: {}", message.correlationId(), message.price());
      return false;
    }
    if (message.region() == null) {
      log.warn("[corr-id={}] Region is null", message.correlationId());
      return false;
    }
    if (message.orderType() == null
        || (!message.orderType().equals("BUY") && !message.orderType().equals("SELL"))) {
      log.warn("[corr-id={}] Invalid order type: {}", message.correlationId(), message.orderType());
      return false;
    }

    // Trading limits validation
    // Price limits: 0.01 - 500 EUR/MWh (electricity can go negative but we simplify here)
    BigDecimal minPrice = new BigDecimal("0.01");
    BigDecimal maxPrice = new BigDecimal("500.00");
    if (message.price().compareTo(minPrice) < 0 || message.price().compareTo(maxPrice) > 0) {
      log.warn(
          "[corr-id={}] Price out of range (0.01-500): {}",
          message.correlationId(),
          message.price());
      return false;
    }

    // Quantity limits: 0.1 - 1000 MWh
    BigDecimal minQuantity = new BigDecimal("0.1");
    BigDecimal maxQuantity = new BigDecimal("1000.00");
    if (message.quantity().compareTo(minQuantity) < 0
        || message.quantity().compareTo(maxQuantity) > 0) {
      log.warn(
          "[corr-id={}] Quantity out of range (0.1-1000): {}",
          message.correlationId(),
          message.quantity());
      return false;
    }

    return true;
  }
}
