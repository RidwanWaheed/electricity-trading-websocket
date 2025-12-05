package com.trading.order.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.OrderStatus;
import com.trading.common.messaging.OrderStatusMessage;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order status updates to Gateway.
 *
 * <p>Message flow: Order Service → orders.topic (order.status.{userId}) → Gateway → WebSocket
 *
 * <p>The routing key includes the username so Gateway can route updates to the correct WebSocket
 * session. This is the "user-specific messaging" pattern.
 */
@Component
public class StatusPublisher {

  private static final Logger log = LoggerFactory.getLogger(StatusPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public StatusPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Notify Gateway (and ultimately the user) of an order status change.
   *
   * @param correlationId Correlation ID for distributed tracing
   * @param orderId The order that changed
   * @param username The user to notify
   * @param status New status
   * @param message Human-readable status message
   */
  public void publishStatusUpdate(
      String correlationId, String orderId, String username, OrderStatus status, String message) {

    // Build routing key with username for user-specific delivery
    String routingKey = String.format(ROUTING_ORDER_STATUS_PATTERN, username);

    OrderStatusMessage statusMessage =
        new OrderStatusMessage(correlationId, orderId, username, status, message, Instant.now());

    log.info(
        "[corr-id={}] Publishing status update: orderId={}, status={}, user={}",
        correlationId,
        orderId,
        status,
        username);

    rabbitTemplate.convertAndSend(ORDERS_EXCHANGE, routingKey, statusMessage);
  }
}
