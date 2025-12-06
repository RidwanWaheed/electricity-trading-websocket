package com.trading.priceMonitor.messaging;

import static com.trading.common.messaging.RabbitMQConstants.QUEUE_PRICES;

import com.trading.common.messaging.PriceUpdate;
import com.trading.priceMonitor.model.ElectricityPrice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Listens for price updates from Mock M7 and broadcasts to WebSocket clients.
 *
 * <p>Message flow: Mock M7 → prices.topic → Gateway → WebSocket → Browser
 */
@Component
public class PriceListener {

  private static final Logger log = LoggerFactory.getLogger(PriceListener.class);

  /** WebSocket destination for price broadcasts */
  private static final String WEBSOCKET_PRICES_TOPIC = "/topic/prices";

  /** Area name for the electricity market (could be configurable) */
  private static final String MARKET_AREA = "Germany";

  private final SimpMessagingTemplate messagingTemplate;

  public PriceListener(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Receive price update from Mock M7 and broadcast to WebSocket clients.
   *
   * <p>Converts the RabbitMQ message to the WebSocket format expected by the frontend.
   */
  @RabbitListener(queues = QUEUE_PRICES)
  public void onPriceUpdate(PriceUpdate update) {
    log.debug(
        "Received price update: region={}, price={} {}",
        update.region(),
        update.price(),
        update.currency());

    ElectricityPrice wsMessage =
        new ElectricityPrice(
            MARKET_AREA,
            update.region(),
            update.price(),
            update.currency(),
            update.changePercent(),
            update.timestamp());

    messagingTemplate.convertAndSend(WEBSOCKET_PRICES_TOPIC, wsMessage);
  }
}
