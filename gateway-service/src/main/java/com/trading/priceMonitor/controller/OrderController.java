package com.trading.priceMonitor.controller;

import com.trading.common.OrderStatus;
import com.trading.common.messaging.OrderSubmitMessage;
import com.trading.priceMonitor.messaging.OrderPublisher;
import com.trading.priceMonitor.model.Order;
import com.trading.priceMonitor.model.OrderConfirmation;
import com.trading.priceMonitor.service.BalanceService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
import org.springframework.validation.annotation.Validated;

/**
 * WebSocket controller for order submissions.
 *
 * <p>Receives orders via STOMP, validates trading limits and balance, generates a correlation ID
 * for tracking, sends an immediate acknowledgment to the user, then publishes to Order Service via
 * RabbitMQ.
 *
 * <p><b>Why @Validated on the class?</b> Spring requires @Validated on the class level to enable
 * method parameter validation for non-REST controllers (like WebSocket @MessageMapping methods).
 */
@Controller
@Validated
public class OrderController {

  private static final Logger log = LoggerFactory.getLogger(OrderController.class);

  // Note: Trading limits are now defined as JSR-380 annotations in the Order record.
  // This keeps validation rules in one place (the DTO) rather than scattered in controllers.

  private final SimpMessagingTemplate messagingTemplate;
  private final OrderPublisher orderPublisher;
  private final BalanceService balanceService;

  public OrderController(
      SimpMessagingTemplate messagingTemplate,
      OrderPublisher orderPublisher,
      BalanceService balanceService) {
    this.messagingTemplate = messagingTemplate;
    this.orderPublisher = orderPublisher;
    this.balanceService = balanceService;
  }

  /**
   * Handles order submissions from WebSocket clients.
   *
   * <p>Flow:
   *
   * <ol>
   *   <li>Validate order using JSR-380 annotations (@Valid)
   *   <li>For BUY orders: check and reserve balance
   *   <li>Generate correlation ID for distributed tracing
   *   <li>Send immediate PENDING acknowledgment to user
   *   <li>Publish order to Order Service via RabbitMQ
   * </ol>
   */
  @MessageMapping("/order")
  public void handleOrder(@Valid Order order, Principal principal) {
    String username = extractUsername(principal);
    String correlationId = generateCorrelationId();

    log.info(
        "[corr-id={}] Order received via WebSocket: orderId={}, user={}",
        correlationId,
        order.orderId(),
        username);

    BigDecimal orderValue = order.price().multiply(order.quantity());

    // For BUY orders, check and reserve balance
    if ("BUY".equals(order.type())) {
      if (!balanceService.reserveBalance(order.orderId(), username, orderValue)) {
        log.warn("[corr-id={}] Insufficient balance for BUY order", correlationId);
        sendRejection(username, order.orderId(), "Insufficient balance");
        return;
      }
      log.info("[corr-id={}] Reserved {} from balance for BUY order", correlationId, orderValue);
    }

    // For SELL orders, track for later credit when filled
    if ("SELL".equals(order.type())) {
      balanceService.trackSellOrder(order.orderId(), username, orderValue);
      log.info(
          "[corr-id={}] Tracking SELL order for {} credit when filled", correlationId, orderValue);
    }

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

  /** Send a REJECTED confirmation to the user. */
  private void sendRejection(String username, String orderId, String reason) {
    OrderConfirmation rejection =
        new OrderConfirmation(orderId, OrderStatus.REJECTED, reason, Instant.now());
    messagingTemplate.convertAndSendToUser(username, "/queue/order-confirmation", rejection);
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
