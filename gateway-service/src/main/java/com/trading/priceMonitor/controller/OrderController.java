package com.trading.priceMonitor.controller;

import com.trading.common.OrderStatus;
import com.trading.common.messaging.OrderSubmitMessage;
import com.trading.priceMonitor.messaging.OrderPublisher;
import com.trading.priceMonitor.model.Order;
import com.trading.priceMonitor.model.OrderConfirmation;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for order submissions.
 *
 * <p>Receives orders via STOMP, generates a correlation ID for tracking, sends an immediate
 * acknowledgment to the user, then publishes to Order Service via RabbitMQ.
 */
@Controller
public class OrderController {

  private static final Logger log = LoggerFactory.getLogger(OrderController.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final OrderPublisher orderPublisher;

  public OrderController(SimpMessagingTemplate messagingTemplate, OrderPublisher orderPublisher) {
    this.messagingTemplate = messagingTemplate;
    this.orderPublisher = orderPublisher;
  }

  /**
   * Handles order submissions from WebSocket clients.
   *
   * <p>Flow:
   *
   * <ol>
   *   <li>Generate correlation ID for distributed tracing
   *   <li>Send immediate PENDING acknowledgment to user
   *   <li>Publish order to Order Service via RabbitMQ
   * </ol>
   */
  @MessageMapping("/order")
  public void handleOrder(Order order, Principal principal) {
    String username = extractUsername(principal);
    String correlationId = generateCorrelationId();

    log.info(
        "[corr-id={}] Order received via WebSocket: orderId={}, user={}",
        correlationId,
        order.orderId(),
        username);

    // Send immediate PENDING acknowledgment before async processing
    OrderConfirmation acknowledgment =
        new OrderConfirmation(
            order.orderId(),
            OrderStatus.PENDING,
            "Order received, routing to Order Service...",
            Instant.now());
    messagingTemplate.convertAndSendToUser(username, "/queue/order-confirmation", acknowledgment);

    // Create message for Order Service with correlation ID
    OrderSubmitMessage message =
        new OrderSubmitMessage(
            correlationId,
            order.orderId(),
            username,
            order.region(),
            order.type(),
            order.quantity(),
            order.price());

    orderPublisher.publish(message);
  }

  /**
   * Generates a unique correlation ID for tracking an order across all services.
   *
   * <p>This ID will appear in logs of Gateway, Order Service, and Mock M7, making it easy to trace
   * the complete journey of an order.
   */
  private String generateCorrelationId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private String extractUsername(Principal principal) {
    if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
      return principal.getName();
    }
    return "anonymous";
  }

  @MessageExceptionHandler
  @SendToUser("/queue/errors")
  public String handleException(Exception ex) {
    log.error("Error processing order", ex);
    return "Error processing your request: " + ex.getMessage();
  }
}
