package com.trading.priceMonitor.config;

import static com.trading.common.messaging.RabbitMQConstants.*;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for Gateway Service.
 *
 * <p>Gateway's role in the message flow:
 *
 * <ul>
 *   <li>PUBLISHES to orders.topic with key "order.submit" → Order Service receives
 *   <li>LISTENS on gateway.order-status queue for "order.status.*" → pushes to WebSocket
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

  // ===== EXCHANGES =====
  // Gateway publishes to orders.topic exchange

  @Bean
  public TopicExchange ordersExchange() {
    return new TopicExchange(ORDERS_EXCHANGE);
  }

  // ===== QUEUES =====
  // Gateway listens on this queue for order status updates from Order Service

  @Bean
  public Queue orderStatusQueue() {
    return QueueBuilder.durable(QUEUE_ORDER_STATUS).build();
  }

  // ===== BINDINGS =====
  // Bind queue to exchange with wildcard pattern to receive all status updates

  @Bean
  public Binding orderStatusBinding(Queue orderStatusQueue, TopicExchange ordersExchange) {
    // order.status.* matches order.status.john, order.status.jane, etc.
    return BindingBuilder.bind(orderStatusQueue)
        .to(ordersExchange)
        .with(ROUTING_ORDER_STATUS_WILDCARD);
  }

  // ===== MESSAGE CONVERTER =====
  // Use JSON for message serialization

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
