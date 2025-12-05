package com.trading.common.messaging;

/**
 * RabbitMQ exchange, queue, and routing key constants.
 *
 * <p>All services use these constants to ensure consistent naming.
 *
 * <h2>Message Flow Overview</h2>
 *
 * <pre>
 * 1. Gateway publishes to ORDERS_EXCHANGE with key ROUTING_ORDER_SUBMIT
 * 2. Order Service listens on QUEUE_ORDER_SUBMISSIONS
 * 3. Order Service publishes to M7_EXCHANGE with key ROUTING_M7_REQUEST
 * 4. Mock M7 listens on QUEUE_M7_REQUESTS
 * 5. Mock M7 publishes ACK to M7_EXCHANGE with key ROUTING_M7_ACK
 * 6. Mock M7 publishes FILL to M7_EXCHANGE with key ROUTING_M7_FILL
 * 7. Order Service listens on QUEUE_M7_ACK and QUEUE_M7_FILL
 * 8. Order Service publishes to ORDERS_EXCHANGE with key order.status.{userId}
 * 9. Gateway listens on QUEUE_ORDER_STATUS
 * </pre>
 */
public final class RabbitMQConstants {

  private RabbitMQConstants() {}

  // ===== EXCHANGES =====
  // Topic exchanges route messages based on routing key patterns

  /** Exchange for order-related messages (Gateway ↔ Order Service) */
  public static final String ORDERS_EXCHANGE = "orders.topic";

  /** Exchange for M7 communication (Order Service ↔ Mock M7) */
  public static final String M7_EXCHANGE = "m7.topic";

  /** Exchange for price updates (Mock M7 → Gateway) */
  public static final String PRICES_EXCHANGE = "prices.topic";

  // ===== QUEUES =====

  /** Order Service listens here for new orders from Gateway */
  public static final String QUEUE_ORDER_SUBMISSIONS = "order.submissions";

  /** Order Service listens here for M7 ACK responses */
  public static final String QUEUE_M7_ACK = "order.m7-ack";

  /** Order Service listens here for M7 FILL responses */
  public static final String QUEUE_M7_FILL = "order.m7-fill";

  /** Mock M7 listens here for order requests */
  public static final String QUEUE_M7_REQUESTS = "m7.requests";

  /** Gateway listens here for order status updates */
  public static final String QUEUE_ORDER_STATUS = "gateway.order-status";

  /** Gateway listens here for price updates from Mock M7 */
  public static final String QUEUE_PRICES = "gateway.prices";

  // ===== ROUTING KEYS =====

  /** New order submission from Gateway */
  public static final String ROUTING_ORDER_SUBMIT = "order.submit";

  /**
   * Order status update pattern. Use with String.format() to add userId. Example:
   * String.format(ROUTING_ORDER_STATUS_PATTERN, "john") → "order.status.john"
   */
  public static final String ROUTING_ORDER_STATUS_PATTERN = "order.status.%s";

  /** Wildcard binding for Gateway to receive all status updates */
  public static final String ROUTING_ORDER_STATUS_WILDCARD = "order.status.*";

  /** Order request to M7 */
  public static final String ROUTING_M7_REQUEST = "m7.request.order";

  /** M7 acknowledgment response */
  public static final String ROUTING_M7_ACK = "m7.response.ack";

  /** M7 fill/reject response */
  public static final String ROUTING_M7_FILL = "m7.response.fill";

  /** Wildcard binding for Order Service to receive all M7 responses */
  public static final String ROUTING_M7_RESPONSE_WILDCARD = "m7.response.*";

  // ===== PRICE ROUTING KEYS =====

  /**
   * Price update routing key pattern. Use with String.format() to add region. Example:
   * String.format(ROUTING_PRICE_PATTERN, "NORTH") → "price.NORTH"
   */
  public static final String ROUTING_PRICE_PATTERN = "price.%s";

  /** Wildcard binding for Gateway to receive all price updates */
  public static final String ROUTING_PRICE_WILDCARD = "price.*";
}
