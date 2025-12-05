package com.trading.mockm7.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.messaging.M7OrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for order requests from Order Service.
 *
 * <p>Message flow: Order Service → m7.topic (m7.request.order) → Mock M7
 *
 * <p>This is the entry point for orders into the simulated trading engine.
 */
@Component
public class OrderRequestListener {

  private static final Logger log = LoggerFactory.getLogger(OrderRequestListener.class);

  private final TradingEngineSimulator tradingEngine;

  public OrderRequestListener(TradingEngineSimulator tradingEngine) {
    this.tradingEngine = tradingEngine;
  }

  /**
   * Handle incoming order request.
   *
   * <p>Delegates to the trading engine simulator which handles ACK and FILL responses.
   */
  @RabbitListener(queues = QUEUE_M7_REQUESTS)
  public void onOrderRequest(M7OrderRequest request) {
    log.info(
        "[corr-id={}] Received order request: orderId={}, region={}, type={}",
        request.correlationId(),
        request.orderId(),
        request.region(),
        request.orderType());

    tradingEngine.processOrder(request);
  }
}
