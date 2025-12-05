package com.trading.order.config;

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
 * RabbitMQ configuration for Order Service.
 *
 * <p>Order Service's role in the message flow:
 *
 * <ul>
 *   <li>LISTENS on order.submissions for orders from Gateway
 *   <li>PUBLISHES to m7.topic to send orders to Mock M7
 *   <li>LISTENS on order.m7-responses for ACK/FILL from Mock M7
 *   <li>PUBLISHES to orders.topic to notify Gateway of status changes
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

  // ===== EXCHANGES =====

  /** Exchange for order flow (Gateway ↔ Order Service) */
  @Bean
  public TopicExchange ordersExchange() {
    return new TopicExchange(ORDERS_EXCHANGE);
  }

  /** Exchange for M7 communication (Order Service ↔ Mock M7) */
  @Bean
  public TopicExchange m7Exchange() {
    return new TopicExchange(M7_EXCHANGE);
  }

  // ===== QUEUES =====

  /**
   * Queue for receiving orders from Gateway.
   *
   * <p>Why durable? So messages survive RabbitMQ restarts.
   */
  @Bean
  public Queue orderSubmissionsQueue() {
    return QueueBuilder.durable(QUEUE_ORDER_SUBMISSIONS).build();
  }

  /**
   * Queue for receiving M7 responses (ACK and FILL).
   *
   * <p>Uses wildcard binding to receive both m7.response.ack and m7.response.fill
   */
  @Bean
  public Queue m7ResponsesQueue() {
    return QueueBuilder.durable(QUEUE_M7_RESPONSES).build();
  }

  // ===== BINDINGS =====

  /** Bind order submissions queue to orders exchange */
  @Bean
  public Binding orderSubmissionsBinding(
      Queue orderSubmissionsQueue, TopicExchange ordersExchange) {
    return BindingBuilder.bind(orderSubmissionsQueue).to(ordersExchange).with(ROUTING_ORDER_SUBMIT);
  }

  /**
   * Bind M7 responses queue to M7 exchange with wildcard.
   *
   * <p>m7.response.* matches both m7.response.ack and m7.response.fill
   */
  @Bean
  public Binding m7ResponsesBinding(Queue m7ResponsesQueue, TopicExchange m7Exchange) {
    return BindingBuilder.bind(m7ResponsesQueue).to(m7Exchange).with(ROUTING_M7_RESPONSE_WILDCARD);
  }

  // ===== MESSAGE CONVERTER =====

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
