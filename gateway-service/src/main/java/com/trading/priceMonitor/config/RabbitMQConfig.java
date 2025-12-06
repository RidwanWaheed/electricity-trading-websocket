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
 *   <li>LISTENS on gateway.order-status for "order.status.*" → pushes to WebSocket
 *   <li>LISTENS on gateway.prices for "price.*" → broadcasts to WebSocket
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

  /** Exchange for order messages */
  @Bean
  public TopicExchange ordersExchange() {
    return new TopicExchange(ORDERS_EXCHANGE);
  }

  /** Exchange for price updates from Mock M7 */
  @Bean
  public TopicExchange pricesExchange() {
    return new TopicExchange(PRICES_EXCHANGE);
  }

  /** Queue for order status updates from Order Service */
  @Bean
  public Queue orderStatusQueue() {
    return QueueBuilder.durable(QUEUE_ORDER_STATUS).build();
  }

  /** Queue for price updates from Mock M7 */
  @Bean
  public Queue pricesQueue() {
    return QueueBuilder.durable(QUEUE_PRICES).build();
  }

  /** Bind order status queue - receives all user status updates */
  @Bean
  public Binding orderStatusBinding(Queue orderStatusQueue, TopicExchange ordersExchange) {
    return BindingBuilder.bind(orderStatusQueue)
        .to(ordersExchange)
        .with(ROUTING_ORDER_STATUS_WILDCARD);
  }

  /** Bind prices queue - receives all regional price updates */
  @Bean
  public Binding pricesBinding(Queue pricesQueue, TopicExchange pricesExchange) {
    // price.* matches price.NORTH, price.SOUTH, etc.
    return BindingBuilder.bind(pricesQueue).to(pricesExchange).with(ROUTING_PRICE_WILDCARD);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
