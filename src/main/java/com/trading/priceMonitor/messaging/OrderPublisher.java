package com.trading.priceMonitor.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.trading.priceMonitor.config.RabbitMQConfig;
import com.trading.priceMonitor.dto.OrderMessage;

/**
 * Publishes order messages to RabbitMQ.
 *
 * This is the PRODUCER in the messaging pattern:
 * - Receives orders from WebSocket controller
 * - Sends them to the exchange (which routes to the queue)
 * - Does NOT process orders - that's the consumer's job
 *
 * Why separate Publisher from Controller?
 * - Single Responsibility: Controller handles HTTP/WebSocket, Publisher handles messaging
 * - Testability: Can mock Publisher in controller tests
 * - Flexibility: Could switch from RabbitMQ to Kafka without changing controller
 */
@Component
public class OrderPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes an order to the message queue.
     *
     * @param message The order message containing order details and username
     */
    public void publish(OrderMessage message) {
        log.info("Publishing order to queue: orderId={}, user={}",
                message.orderId(), message.username());

        // convertAndSend does three things:
        // 1. Converts OrderMessage to JSON (using our Jackson converter)
        // 2. Sends to the EXCHANGE (not directly to queue!)
        // 3. Uses ROUTING_KEY to determine which queue(s) receive it
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                message
        );

        log.debug("Order published successfully: {}", message.orderId());
    }
}
