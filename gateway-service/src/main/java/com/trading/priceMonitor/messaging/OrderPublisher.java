package com.trading.priceMonitor.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.messaging.OrderSubmitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order submission messages to RabbitMQ for Order Service to process.
 *
 * <p>Message flow: Gateway → orders.topic (order.submit) → Order Service
 */
@Component
public class OrderPublisher {

  private static final Logger log = LoggerFactory.getLogger(OrderPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public OrderPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Publishes an order to the Order Service via RabbitMQ.
   *
   * @param message The order submission message with correlation ID
   */
  public void publish(OrderSubmitMessage message) {
    log.info(
        "[corr-id={}] Publishing order to Order Service: orderId={}, user={}",
        message.correlationId(),
        message.orderId(),
        message.username());

    rabbitTemplate.convertAndSend(ORDERS_EXCHANGE, ROUTING_ORDER_SUBMIT, message);

    log.debug("[corr-id={}] Order published successfully", message.correlationId());
  }
}
