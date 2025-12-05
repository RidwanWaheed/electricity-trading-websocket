package com.trading.priceMonitor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration defining queues, exchanges, and bindings.
 *
 * Message flow:
 * 1. Producer sends message to EXCHANGE with a routing key (e.g., "order.created")
 * 2. Exchange routes message to QUEUE based on the BINDING pattern
 * 3. Consumer listens on QUEUE and processes messages
 *
 * Why Topic Exchange?
 * - Allows pattern matching: "order.*" matches "order.created", "order.cancelled"
 * - Flexible routing without tight coupling between producer and consumer
 * - In real trading: different queues for different order types/regions
 */
@Configuration
public class RabbitMQConfig {

    // Queue name - where order messages wait to be processed
    public static final String ORDER_QUEUE = "trading.orders";

    // Exchange name - the router that directs messages to queues
    public static final String ORDER_EXCHANGE = "trading.exchange";

    // Routing key - the pattern used to match messages to queues
    public static final String ORDER_ROUTING_KEY = "order.created";

    /**
     * Creates a durable queue.
     * Durable = survives broker restart (messages not lost if RabbitMQ restarts)
     */
    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);  // true = durable
    }

    /**
     * Creates a topic exchange.
     * Topic exchange routes based on pattern matching:
     * - "order.created" matches exactly "order.created"
     * - "order.*" would match "order.created", "order.cancelled", etc.
     * - "order.#" would match "order.created.urgent" (# = zero or more words)
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    /**
     * Binds the queue to the exchange with a routing key.
     * This tells RabbitMQ: "When a message arrives at the exchange with
     * routing key 'order.created', put it in the order queue"
     */
    @Bean
    public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY);
    }

    /**
     * Converts Java objects to JSON when sending, and JSON back to Java when receiving.
     * Without this, RabbitMQ would use Java serialization (hard to debug, version issues)
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
