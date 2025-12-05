package com.trading.priceMonitor.messaging;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.trading.priceMonitor.config.RabbitMQConfig;
import com.trading.priceMonitor.dto.OrderMessage;
import com.trading.priceMonitor.entity.OrderEntity;
import com.trading.priceMonitor.model.OrderConfirmation;
import com.trading.priceMonitor.model.Status;
import com.trading.priceMonitor.service.OrderService;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderConsumer(OrderService orderService, SimpMessagingTemplate messagingTemplate) {
        this.orderService = orderService;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void consume(OrderMessage message) {
        log.info("Received order from queue: orderId={}, user={}",
                message.orderId(), message.username());

        Optional<OrderEntity> result = orderService.processOrder(message);

        OrderConfirmation confirmation;
        if (result.isPresent()) {
            OrderEntity order = result.get();
            String statusMessage = order.getStatus() == Status.ACCEPTED
                    ? "Order accepted and persisted"
                    : "Order rejected: validation failed";

            confirmation = new OrderConfirmation(
                    order.getOrderId(),
                    order.getStatus(),
                    statusMessage,
                    Instant.now()
            );
        } else {
            confirmation = new OrderConfirmation(
                    message.orderId(),
                    Status.REJECTED,
                    "Order processing failed",
                    Instant.now()
            );
        }

        sendConfirmation(message.username(), confirmation);
    }

    private void sendConfirmation(String username, OrderConfirmation confirmation) {
        if (username == null || username.isBlank()) {
            log.error("Cannot send confirmation: username is null or blank");
            return;
        }

        log.info("Sending confirmation to user: user={}, orderId={}, status={}",
                username, confirmation.orderId(), confirmation.status());

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/order-confirmation",
                confirmation
        );
    }
}
