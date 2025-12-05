package com.trading.priceMonitor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String ORDER_QUEUE = "trading.orders";
  public static final String ORDER_EXCHANGE = "trading.exchange";
  public static final String ORDER_ROUTING_KEY = "order.created";

  public static final String DLQ_QUEUE = "trading.orders.dlq";
  public static final String DLQ_EXCHANGE = "trading.dlx";
  public static final String DLQ_ROUTING_KEY = "order.failed";

  // Failed messages are automatically routed to the DLQ via x-dead-letter-* args
  @Bean
  public Queue orderQueue() {
    return QueueBuilder.durable(ORDER_QUEUE)
        .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
        .build();
  }

  @Bean
  public TopicExchange orderExchange() {
    return new TopicExchange(ORDER_EXCHANGE);
  }

  @Bean
  public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) {
    return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
  }

  @Bean
  public Queue deadLetterQueue() {
    return QueueBuilder.durable(DLQ_QUEUE).build();
  }

  @Bean
  public DirectExchange deadLetterExchange() {
    return new DirectExchange(DLQ_EXCHANGE);
  }

  @Bean
  public Binding dlqBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
    return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ_ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
