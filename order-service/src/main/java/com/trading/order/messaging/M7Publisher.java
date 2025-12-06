package com.trading.order.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.messaging.M7OrderRequest;
import com.trading.order.entity.OrderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes orders to Mock M7 (trading engine).
 *
 * <p>Message flow: Order Service → m7.topic (m7.request.order) → Mock M7
 *
 * <p>This simulates sending an order to the real M7/EPEX SPOT trading engine. The correlation ID is
 * preserved so we can track the order's journey.
 */
@Component
public class M7Publisher {

  private static final Logger log = LoggerFactory.getLogger(M7Publisher.class);

  private final RabbitTemplate rabbitTemplate;

  public M7Publisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publishOrder(OrderEntity order) {
    M7OrderRequest request =
        new M7OrderRequest(
            order.getCorrelationId(),
            order.getOrderId(),
            order.getRegion(),
            order.getOrderType(),
            order.getQuantity(),
            order.getPrice());

    log.info(
        "[corr-id={}] Publishing order to M7: orderId={}, type={}, quantity={}, price={}",
        order.getCorrelationId(),
        order.getOrderId(),
        order.getOrderType(),
        order.getQuantity(),
        order.getPrice());

    rabbitTemplate.convertAndSend(M7_EXCHANGE, ROUTING_M7_REQUEST, request);
  }
}
