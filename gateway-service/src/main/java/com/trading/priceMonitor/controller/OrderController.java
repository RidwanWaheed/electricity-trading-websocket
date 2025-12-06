package com.trading.priceMonitor.controller;

import com.trading.common.OrderStatus;
import com.trading.common.messaging.OrderSubmitMessage;
import com.trading.priceMonitor.messaging.OrderPublisher;
import com.trading.priceMonitor.model.Order;
import com.trading.priceMonitor.model.OrderConfirmation;
import com.trading.priceMonitor.service.BalanceService;
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

/**
 * WebSocket controller for order submissions.
 *
 * <p>Receives orders via STOMP, validates trading limits and balance, generates a correlation ID
 * for tracking, sends an immediate acknowledgment to the user, then publishes to Order Service via
 * RabbitMQ.
 */
@Controller
public class OrderController {

  private static final Logger log = LoggerFactory.getLogger(OrderController.class);

  // Trading limits (must match Order Service validation)
  private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
  private static final BigDecimal MAX_PRICE = new BigDecimal("500.00");
  private static final BigDecimal MIN_QUANTITY = new BigDecimal("0.1");
  private static final BigDecimal MAX_QUANTITY = new BigDecimal("1000.00");

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
   *   <li>Validate trading limits (price, quantity)
   *   <li>For BUY orders: check and reserve balance
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

    // Validate trading limits first
    String validationError = validateTradingLimits(order);
    if (validationError != null) {
      log.warn("[corr-id={}] Validation failed: {}", correlationId, validationError);
      sendRejection(username, order.orderId(), validationError);
      return;
    }

    // For BUY orders, check and reserve balance
    if ("BUY".equals(order.type())) {
      BigDecimal orderCost = order.price().multiply(order.quantity());
      if (!balanceService.reserveBalance(order.orderId(), username, orderCost)) {
        log.warn("[corr-id={}] Insufficient balance for BUY order", correlationId);
        sendRejection(username, order.orderId(), "Insufficient balance");
        return;
      }
      log.info("[corr-id={}] Reserved {} from balance for BUY order", correlationId, orderCost);
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

  /**
   * Validate trading limits.
   *
   * @return null if valid, error message if invalid
   */
  private String validateTradingLimits(Order order) {
    if (order.price() == null || order.price().compareTo(MIN_PRICE) < 0) {
      return "Price must be at least " + MIN_PRICE + " EUR/MWh";
    }
    if (order.price().compareTo(MAX_PRICE) > 0) {
      return "Price cannot exceed " + MAX_PRICE + " EUR/MWh";
    }
    if (order.quantity() == null || order.quantity().compareTo(MIN_QUANTITY) < 0) {
      return "Quantity must be at least " + MIN_QUANTITY + " MWh";
    }
    if (order.quantity().compareTo(MAX_QUANTITY) > 0) {
      return "Quantity cannot exceed " + MAX_QUANTITY + " MWh";
    }
    return null;
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
