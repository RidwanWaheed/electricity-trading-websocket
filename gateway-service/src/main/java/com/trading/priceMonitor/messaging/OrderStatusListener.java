package com.trading.priceMonitor.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.messaging.OrderStatusMessage;
import com.trading.priceMonitor.model.OrderConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Listens for order status updates from Order Service and pushes them to WebSocket clients.
 *
 * <p>Message flow: Order Service → orders.topic (order.status.*) → Gateway → WebSocket → Browser
 */
@Component
public class OrderStatusListener {

  private static final Logger log = LoggerFactory.getLogger(OrderStatusListener.class);

  private final SimpMessagingTemplate messagingTemplate;

  public OrderStatusListener(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Receives order status updates from Order Service via RabbitMQ.
   *
   * <p>Converts the message to an OrderConfirmation and sends it to the specific user's WebSocket
   * session.
   */
  @RabbitListener(queues = QUEUE_ORDER_STATUS)
  public void onOrderStatus(OrderStatusMessage message) {
    log.info(
        "[corr-id={}] Received status update: orderId={}, status={}, user={}",
        message.correlationId(),
        message.orderId(),
        message.status(),
        message.username());

    // Convert to WebSocket-friendly format
    OrderConfirmation confirmation =
        new OrderConfirmation(
            message.orderId(), message.status(), message.message(), message.timestamp());

    // Push to the specific user's WebSocket session
    messagingTemplate.convertAndSendToUser(
        message.username(), "/queue/order-confirmation", confirmation);

    log.debug(
        "[corr-id={}] Status pushed to WebSocket for user {}",
        message.correlationId(),
        message.username());
  }
}
