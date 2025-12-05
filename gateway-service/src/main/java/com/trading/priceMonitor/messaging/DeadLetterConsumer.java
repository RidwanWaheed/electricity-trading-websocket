package com.trading.priceMonitor.messaging;

import com.trading.priceMonitor.config.RabbitMQConfig;
import com.trading.priceMonitor.dto.OrderMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Handles messages that failed processing in the main queue.
 *
 * <p>Production enhancements to consider: - Save to a failed_orders table for investigation - Send
 * alerts (Slack, PagerDuty) for critical failures - Implement retry logic with exponential backoff
 * - Build an admin UI to view/retry/discard failed messages
 */
@Component
public class DeadLetterConsumer {

  private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

  @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
  public void handleFailedMessage(OrderMessage message) {
    // TODO: In production, persist to database and trigger alerts
    log.error(
        "Failed order moved to DLQ: orderId={}, user={}, region={}, type={}, qty={}, price={}",
        message.orderId(),
        message.username(),
        message.region(),
        message.orderType(),
        message.quantity(),
        message.price());
  }
}
