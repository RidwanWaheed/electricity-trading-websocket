package com.trading.priceMonitor.controller;

import java.security.Principal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.trading.priceMonitor.dto.OrderMessage;
import com.trading.priceMonitor.messaging.OrderPublisher;
import com.trading.priceMonitor.model.Order;
import com.trading.priceMonitor.model.OrderConfirmation;
import com.trading.priceMonitor.model.Status;

@Controller
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderPublisher orderPublisher;

    public OrderController(SimpMessagingTemplate messagingTemplate, OrderPublisher orderPublisher) {
        this.messagingTemplate = messagingTemplate;
        this.orderPublisher = orderPublisher;
    }

    @MessageMapping("/order")
    public void handleOrder(Order order, Principal principal) {
        String username = extractUsername(principal);

        log.info("Order received via WebSocket: orderId={}, user={}",
                order.orderId(), username);

        // Send immediate PENDING acknowledgment before async processing
        OrderConfirmation acknowledgment = new OrderConfirmation(
                order.orderId(),
                Status.PENDING,
                "Order received, processing...",
                Instant.now()
        );
        messagingTemplate.convertAndSendToUser(username, "/queue/order-confirmation", acknowledgment);

        // Transform to internal message with server-validated username
        OrderMessage message = new OrderMessage(
                order.orderId(),
                username,
                order.region(),
                order.type(),
                order.quantity(),
                order.price()
        );

        orderPublisher.publish(message);
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
