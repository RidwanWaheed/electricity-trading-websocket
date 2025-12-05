package com.trading.mockm7.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.messaging.M7AckResponse;
import com.trading.common.messaging.M7FillResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes M7 responses to Order Service.
 *
 * <p>Two types of responses:
 *
 * <ul>
 *   <li>ACK - Immediate acknowledgment that order was received
 *   <li>FILL - Order execution result (filled or rejected)
 * </ul>
 */
@Component
public class M7ResponsePublisher {

  private static final Logger log = LoggerFactory.getLogger(M7ResponsePublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public M7ResponsePublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Send immediate acknowledgment that order was received.
   *
   * <p>This is sent within milliseconds of receiving the order.
   */
  public void publishAck(M7AckResponse ack) {
    log.info(
        "[corr-id={}] Publishing ACK: orderId={}, m7RefId={}",
        ack.correlationId(),
        ack.orderId(),
        ack.m7ReferenceId());

    rabbitTemplate.convertAndSend(M7_EXCHANGE, ROUTING_M7_ACK, ack);
  }

  /**
   * Send fill/reject response after order execution.
   *
   * <p>This is sent after a simulated delay (500ms - 2s) representing market execution time.
   */
  public void publishFill(M7FillResponse fill) {
    log.info(
        "[corr-id={}] Publishing FILL: orderId={}, filled={}",
        fill.correlationId(),
        fill.orderId(),
        fill.filled());

    rabbitTemplate.convertAndSend(M7_EXCHANGE, ROUTING_M7_FILL, fill);
  }
}
