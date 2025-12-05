package com.trading.order.messaging;

import static com.trading.common.messaging.RabbitMQConstants.*;

import com.trading.common.OrderStatus;
import com.trading.common.messaging.M7AckResponse;
import com.trading.common.messaging.M7FillResponse;
import com.trading.order.entity.OrderEntity;
import com.trading.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for responses from Mock M7 (trading engine).
 *
 * <p>M7 sends two types of responses:
 *
 * <ul>
 *   <li>ACK - Immediate acknowledgment that order was received
 *   <li>FILL - Order was executed (filled) or rejected
 * </ul>
 *
 * <p>Note: We use a single queue (order.m7-responses) with wildcard binding to receive both
 * message types. Spring AMQP routes to the correct method based on message type.
 */
@Component
public class M7ResponseListener {

  private static final Logger log = LoggerFactory.getLogger(M7ResponseListener.class);

  private final OrderRepository orderRepository;
  private final StatusPublisher statusPublisher;

  public M7ResponseListener(OrderRepository orderRepository, StatusPublisher statusPublisher) {
    this.orderRepository = orderRepository;
    this.statusPublisher = statusPublisher;
  }

  /**
   * Handle M7 acknowledgment response.
   *
   * <p>This means M7 received our order. We update status to SUBMITTED.
   *
   * <p>Handles duplicate ACKs gracefully - if order is already SUBMITTED or beyond, we log and
   * ignore (idempotent behavior).
   */
  @RabbitListener(queues = QUEUE_M7_RESPONSES, id = "m7AckListener")
  public void onM7Ack(M7AckResponse response) {
    log.info(
        "[corr-id={}] Received M7 ACK: orderId={}, m7RefId={}",
        response.correlationId(),
        response.orderId(),
        response.m7ReferenceId());

    orderRepository
        .findByOrderId(response.orderId())
        .ifPresentOrElse(
            order -> {
              try {
                order.markSubmitted(response.m7ReferenceId());
                orderRepository.save(order);
                log.info(
                    "[corr-id={}] Order status updated to SUBMITTED", response.correlationId());

                statusPublisher.publishStatusUpdate(
                    response.correlationId(),
                    response.orderId(),
                    order.getUsername(),
                    OrderStatus.SUBMITTED,
                    "Order acknowledged by trading engine (M7 ref: "
                        + response.m7ReferenceId()
                        + ")");
              } catch (IllegalStateException e) {
                // Duplicate ACK or order already processed - log and ignore (idempotent)
                log.warn(
                    "[corr-id={}] Ignoring ACK - order already in state {}: {}",
                    response.correlationId(),
                    order.getStatus(),
                    e.getMessage());
              }
            },
            () ->
                log.error(
                    "[corr-id={}] Order not found for ACK: {}",
                    response.correlationId(),
                    response.orderId()));
  }

  /**
   * Handle M7 fill/reject response.
   *
   * <p>This means M7 has executed (filled) or rejected our order.
   *
   * <p>Handles duplicate or out-of-order messages gracefully - if order is already in a terminal
   * state (FILLED/REJECTED), we log and ignore (idempotent behavior).
   */
  @RabbitListener(queues = QUEUE_M7_RESPONSES, id = "m7FillListener")
  public void onM7Fill(M7FillResponse response) {
    log.info(
        "[corr-id={}] Received M7 FILL response: orderId={}, filled={}",
        response.correlationId(),
        response.orderId(),
        response.filled());

    orderRepository
        .findByOrderId(response.orderId())
        .ifPresentOrElse(
            order -> {
              try {
                if (response.filled()) {
                  order.markFilled(response.executionPrice());
                  orderRepository.save(order);
                  log.info(
                      "[corr-id={}] Order status updated to FILLED", response.correlationId());

                  statusPublisher.publishStatusUpdate(
                      response.correlationId(),
                      response.orderId(),
                      order.getUsername(),
                      OrderStatus.FILLED,
                      "Order filled at " + response.executionPrice() + " EUR/MWh");
                } else {
                  order.markRejected(response.rejectReason());
                  orderRepository.save(order);
                  log.info(
                      "[corr-id={}] Order status updated to REJECTED", response.correlationId());

                  statusPublisher.publishStatusUpdate(
                      response.correlationId(),
                      response.orderId(),
                      order.getUsername(),
                      OrderStatus.REJECTED,
                      "Order rejected: " + response.rejectReason());
                }
              } catch (IllegalStateException e) {
                // Duplicate FILL/REJECT or order in wrong state - log and ignore (idempotent)
                log.warn(
                    "[corr-id={}] Ignoring FILL response - order already in state {}: {}",
                    response.correlationId(),
                    order.getStatus(),
                    e.getMessage());
              }
            },
            () ->
                log.error(
                    "[corr-id={}] Order not found for FILL: {}",
                    response.correlationId(),
                    response.orderId()));
  }
}
