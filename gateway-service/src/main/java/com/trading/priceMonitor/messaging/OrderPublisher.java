package com.trading.priceMonitor.messaging;

import com.trading.priceMonitor.config.RabbitMQConfig;
import com.trading.priceMonitor.dto.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderPublisher {

  private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public OrderPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(OrderMessage message) {
    log.info(
        "Publishing order to queue: orderId={}, user={}", message.orderId(), message.username());

    rabbitTemplate.convertAndSend(
        RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY, message);

    log.debug("Order published successfully: {}", message.orderId());
  }
}
