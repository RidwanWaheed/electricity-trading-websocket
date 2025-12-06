package com.trading.priceMonitor.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.OrderStatus;
import com.trading.common.messaging.OrderStatusMessage;
import com.trading.priceMonitor.model.OrderConfirmation;
import com.trading.priceMonitor.service.BalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Listens for order status updates from Order Service and pushes them to WebSocket clients.
 *
 * <p>Message flow: Order Service → orders.topic (order.status.*) → Gateway → WebSocket → Browser
 *
 * <p>Also handles balance management for completed orders (refund on rejection).
 */
@Component
public class OrderStatusListener {

  private static final Logger log = LoggerFactory.getLogger(OrderStatusListener.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final BalanceService balanceService;

  public OrderStatusListener(
      SimpMessagingTemplate messagingTemplate, BalanceService balanceService) {
    this.messagingTemplate = messagingTemplate;
    this.balanceService = balanceService;
  }

  /**
   * Receives order status updates from Order Service via RabbitMQ.
   *
   * <p>Converts the message to an OrderConfirmation and sends it to the specific user's WebSocket
   * session.
   *
   * <p>Also manages balance: clears reservation on FILLED, refunds on REJECTED.
   */
  @RabbitListener(queues = QUEUE_ORDER_STATUS)
  public void onOrderStatus(OrderStatusMessage message) {
    log.info(
        "[corr-id={}] Received status update: orderId={}, status={}, user={}",
        message.correlationId(),
        message.orderId(),
        message.status(),
        message.username());

    // Handle balance for terminal states
    if (message.status() == OrderStatus.FILLED) {
      // Order completed successfully - clear the reservation tracking
      balanceService.onOrderFilled(message.orderId());
    } else if (message.status() == OrderStatus.REJECTED) {
      // Order rejected - refund any reserved balance
      balanceService.onOrderRejected(message.orderId());
    }

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
