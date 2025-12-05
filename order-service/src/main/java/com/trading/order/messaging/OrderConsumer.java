package com.trading.order.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.OrderStatus;
import com.trading.common.messaging.OrderSubmitMessage;
import com.trading.order.entity.OrderEntity;
import com.trading.order.repository.OrderRepository;
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

    // Validate the order
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

    // Create and save the order entity
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

    // Notify Gateway that order is pending (optional - Gateway already knows)
    // We notify again to confirm Order Service received and processed it
    statusPublisher.publishStatusUpdate(
        message.correlationId(),
        message.orderId(),
        message.username(),
        OrderStatus.PENDING,
        "Order received by Order Service, forwarding to trading engine");

    // Forward to Mock M7 for execution
    m7Publisher.publishOrder(order);
    log.info("[corr-id={}] Order forwarded to M7", message.correlationId());
  }

  /**
   * Validate the order.
   *
   * <p>In a real system, this would check:
   *
   * <ul>
   *   <li>Valid region
   *   <li>Price within acceptable range
   *   <li>Quantity within limits
   *   <li>User has permission to trade
   * </ul>
   */
  private boolean isValidOrder(OrderSubmitMessage message) {
    // Basic validation
    if (message.quantity() == null || message.quantity().signum() <= 0) {
      log.warn("[corr-id={}] Invalid quantity: {}", message.correlationId(), message.quantity());
      return false;
    }
    if (message.price() == null || message.price().signum() <= 0) {
      log.warn("[corr-id={}] Invalid price: {}", message.correlationId(), message.price());
      return false;
    }
    if (message.region() == null || message.region().isBlank()) {
      log.warn("[corr-id={}] Invalid region", message.correlationId());
      return false;
    }
    if (message.orderType() == null
        || (!message.orderType().equals("BUY") && !message.orderType().equals("SELL"))) {
      log.warn("[corr-id={}] Invalid order type: {}", message.correlationId(), message.orderType());
      return false;
    }
    return true;
  }
}
