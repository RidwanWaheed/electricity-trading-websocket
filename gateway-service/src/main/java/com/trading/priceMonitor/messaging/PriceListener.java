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
 * <p>This is the Gateway's role in market data distribution:
 *
 * <ol>
 *   <li>Mock M7 generates prices (simulating exchange)
 *   <li>Gateway receives via RabbitMQ
 *   <li>Gateway broadcasts to all connected WebSocket clients
 * </ol>
 *
 * <p>Message flow: Mock M7 → prices.topic (price.*) → Gateway → WebSocket → Browser
 *
 * <p>Note: Gateway is a pass-through for market data. It doesn't generate or modify prices - it
 * just relays them to clients. This mirrors how real trading systems work.
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

    // Convert to WebSocket message format (includes area for frontend display)
    ElectricityPrice wsMessage =
        new ElectricityPrice(
            MARKET_AREA,
            update.region(),
            update.price(),
            update.currency(),
            update.changePercent(),
            update.timestamp());

    // Broadcast to all connected WebSocket clients
    messagingTemplate.convertAndSend(WEBSOCKET_PRICES_TOPIC, wsMessage);
  }
}
