package com.trading.mockm7.config;

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
 * RabbitMQ configuration for Mock M7 Service.
 *
 * <p>Mock M7's role in the message flow:
 *
 * <ul>
 *   <li>LISTENS on m7.requests for order requests from Order Service
 *   <li>PUBLISHES to m7.topic with m7.response.ack (immediate acknowledgment)
 *   <li>PUBLISHES to m7.topic with m7.response.fill (delayed fill/reject)
 *   <li>PUBLISHES to prices.topic with price updates (market data broadcast)
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

  /** Exchange for M7 order communication */
  @Bean
  public TopicExchange m7Exchange() {
    return new TopicExchange(M7_EXCHANGE);
  }

  /** Exchange for price updates (market data) */
  @Bean
  public TopicExchange pricesExchange() {
    return new TopicExchange(PRICES_EXCHANGE);
  }

  /** Queue for receiving order requests from Order Service */
  @Bean
  public Queue m7RequestsQueue() {
    return QueueBuilder.durable(QUEUE_M7_REQUESTS).build();
  }

  /** Bind M7 requests queue to M7 exchange */
  @Bean
  public Binding m7RequestsBinding(Queue m7RequestsQueue, TopicExchange m7Exchange) {
    return BindingBuilder.bind(m7RequestsQueue).to(m7Exchange).with(ROUTING_M7_REQUEST);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
