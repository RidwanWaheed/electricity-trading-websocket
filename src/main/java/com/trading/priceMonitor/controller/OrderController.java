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

/**
 * WebSocket controller for order handling.
 *
 * Before RabbitMQ: Received order → Processed → Responded (synchronous)
 * After RabbitMQ:  Received order → Published to queue → Acknowledged (async)
 *
 * The actual processing happens in OrderConsumer, which sends the final
 * confirmation back to the user via WebSocket.
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
     * Handles incoming orders via WebSocket.
     *
     * Flow:
     * 1. Extract username from authenticated principal
     * 2. Send immediate acknowledgment (order received, not yet processed)
     * 3. Publish to RabbitMQ queue for async processing
     * 4. Consumer will send final confirmation after processing
     */
    @MessageMapping("/order")
    public void handleOrder(Order order, Principal principal) {
        String username = extractUsername(principal);

        log.info("Order received via WebSocket: orderId={}, user={}",
                order.orderId(), username);

        // Immediate acknowledgment - tells user "we got your order"
        // This is NOT the final confirmation - that comes after processing
        OrderConfirmation acknowledgment = new OrderConfirmation(
                order.orderId(),
                Status.PENDING,
                "Order received, processing...",
                Instant.now()
        );
        messagingTemplate.convertAndSendToUser(username, "/queue/order-confirmation", acknowledgment);

        // Create message with server-validated username and publish to queue
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
