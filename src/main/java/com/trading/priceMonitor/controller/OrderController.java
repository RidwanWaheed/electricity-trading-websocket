package com.trading.priceMonitor.controller;

import java.security.Principal;
import java.time.Instant;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.trading.priceMonitor.model.Order;
import com.trading.priceMonitor.model.OrderConfirmation;
import com.trading.priceMonitor.model.Status;

@Controller
public class OrderController {

  private final SimpMessagingTemplate messagingTemplate;

  public OrderController(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @MessageMapping("/order")
  public void handleOrder(Order order, Principal principal) {
    OrderConfirmation confirmation =
        new OrderConfirmation(order.orderId(), Status.ACCEPTED, "Order accepted", Instant.now());

    // Get user identifier (defaults to "anonymous" without authentication)
    String userId = "anonymous";
    if (principal != null && principal.getName() != null) {
      userId = principal.getName();
    }

    // Send ONLY to the user who placed the order
    // Client subscribes to: /user/queue/order-confirmation
    messagingTemplate.convertAndSendToUser(userId, "/queue/order-confirmation", confirmation);
  }

  @MessageExceptionHandler
  @SendToUser("/queue/errors")
  public String handleException(Exception ex) {
    return "Error processing your request: " + ex.getMessage();
  }
}
