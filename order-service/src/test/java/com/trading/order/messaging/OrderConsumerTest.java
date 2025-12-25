package com.trading.order.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trading.common.OrderStatus;
import com.trading.common.Region;
import com.trading.common.messaging.OrderSubmitMessage;
import com.trading.order.entity.OrderEntity;
import com.trading.order.repository.OrderRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for OrderConsumer.
 *
 * <p>These tests catch real bugs in order validation and processing:
 *
 * <ul>
 *   <li>Null/invalid field handling
 *   <li>Boundary conditions (zero, negative values)
 *   <li>State consistency (correlation ID propagation)
 *   <li>Error handling paths
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

  @Mock private OrderRepository orderRepository;
  @Mock private M7Publisher m7Publisher;
  @Mock private StatusPublisher statusPublisher;
  @InjectMocks private OrderConsumer orderConsumer;

  private OrderSubmitMessage validOrder;

  @BeforeEach
  void setUp() {
    validOrder = createValidOrder("corr-123", "order-456", "trader1");
  }

  private OrderSubmitMessage createValidOrder(String corrId, String orderId, String username) {
    return new OrderSubmitMessage(
        corrId, orderId, username, Region.NORTH, "BUY", new BigDecimal("100"), new BigDecimal("45.50"));
  }

  @Nested
  @DisplayName("Valid Order Processing")
  class ValidOrderProcessing {

    @Test
    @DisplayName("Valid order should be saved with correct correlation ID")
    void validOrder_shouldPreserveCorrelationId() {
      orderConsumer.onOrderSubmit(validOrder);

      ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
      verify(orderRepository).save(captor.capture());

      assertEquals("corr-123", captor.getValue().getCorrelationId());
    }

    @Test
    @DisplayName("Valid order should be saved with PENDING status")
    void validOrder_shouldHavePendingStatus() {
      orderConsumer.onOrderSubmit(validOrder);

      ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
      verify(orderRepository).save(captor.capture());

      assertEquals(OrderStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Valid order should trigger status publish with same correlation ID")
    void validOrder_shouldPublishStatusWithSameCorrelationId() {
      orderConsumer.onOrderSubmit(validOrder);

      verify(statusPublisher)
          .publishStatusUpdate(
              eq("corr-123"), eq("order-456"), eq("trader1"), eq(OrderStatus.PENDING), anyString());
    }

    @Test
    @DisplayName("Valid order should be forwarded to M7")
    void validOrder_shouldBeForwardedToM7() {
      orderConsumer.onOrderSubmit(validOrder);

      ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
      verify(m7Publisher).publishOrder(captor.capture());

      OrderEntity forwarded = captor.getValue();
      assertEquals("order-456", forwarded.getOrderId());
      assertEquals("corr-123", forwarded.getCorrelationId());
    }

    @Test
    @DisplayName("Order processing sequence: save → publish status → forward to M7")
    void validOrder_shouldFollowCorrectSequence() {
      orderConsumer.onOrderSubmit(validOrder);

      var inOrder = inOrder(orderRepository, statusPublisher, m7Publisher);
      inOrder.verify(orderRepository).save(any(OrderEntity.class));
      inOrder.verify(statusPublisher).publishStatusUpdate(any(), any(), any(), any(), any());
      inOrder.verify(m7Publisher).publishOrder(any(OrderEntity.class));
    }
  }

  @Nested
  @DisplayName("Quantity Validation")
  class QuantityValidation {

    @Test
    @DisplayName("Zero quantity should be rejected")
    void zeroQuantity_shouldBeRejected() {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              "BUY",
              BigDecimal.ZERO,
              new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(orderRepository, never()).save(any());
      verify(m7Publisher, never()).publishOrder(any());
      verify(statusPublisher)
          .publishStatusUpdate(any(), any(), any(), eq(OrderStatus.REJECTED), anyString());
    }

    @Test
    @DisplayName("Negative quantity should be rejected")
    void negativeQuantity_shouldBeRejected() {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              "BUY",
              new BigDecimal("-1"),
              new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(orderRepository, never()).save(any());
      verify(statusPublisher)
          .publishStatusUpdate(any(), any(), any(), eq(OrderStatus.REJECTED), anyString());
    }

    @Test
    @DisplayName("Null quantity should be rejected")
    void nullQuantity_shouldBeRejected() {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123", "order-456", "trader1", Region.NORTH, "BUY", null, new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(orderRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Price Validation")
  class PriceValidation {

    @Test
    @DisplayName("Zero price should be rejected")
    void zeroPrice_shouldBeRejected() {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              "BUY",
              new BigDecimal("100"),
              BigDecimal.ZERO);

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Negative price should be rejected")
    void negativePrice_shouldBeRejected() {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              "BUY",
              new BigDecimal("100"),
              new BigDecimal("-0.01"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Very small positive price should be accepted")
    void verySmallPrice_shouldBeAccepted() {
      var validSmallPriceOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              "BUY",
              new BigDecimal("100"),
              new BigDecimal("0.01"));

      orderConsumer.onOrderSubmit(validSmallPriceOrder);

      verify(orderRepository).save(any());
    }
  }

  @Nested
  @DisplayName("Region Validation")
  class RegionValidation {

    @Test
    @DisplayName("Null region should be rejected")
    void nullRegion_shouldBeRejected() {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              null,
              "BUY",
              new BigDecimal("100"),
              new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(orderRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Order Type Validation")
  class OrderTypeValidation {

    @Test
    @DisplayName("BUY order type should be accepted")
    void buyOrderType_shouldBeAccepted() {
      var buyOrder = createValidOrder("corr-1", "order-1", "trader1");

      orderConsumer.onOrderSubmit(buyOrder);

      verify(orderRepository).save(any());
    }

    @Test
    @DisplayName("SELL order type should be accepted")
    void sellOrderType_shouldBeAccepted() {
      var sellOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              "SELL",
              new BigDecimal("100"),
              new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(sellOrder);

      verify(orderRepository).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"buy", "sell", "Buy", "Sell", "MARKET", "LIMIT", "invalid", ""})
    @DisplayName("Invalid order types should be rejected")
    void invalidOrderTypes_shouldBeRejected(String orderType) {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              orderType,
              new BigDecimal("100"),
              new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Null order type should be rejected")
    void nullOrderType_shouldBeRejected() {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              null,
              new BigDecimal("100"),
              new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(orderRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Rejected Order Handling")
  class RejectedOrderHandling {

    @Test
    @DisplayName("Rejected order should still use correct correlation ID")
    void rejectedOrder_shouldUseCorrectCorrelationId() {
      var invalidOrder =
          new OrderSubmitMessage(
              "specific-corr-id",
              "order-456",
              "trader1",
              Region.NORTH,
              "INVALID",
              new BigDecimal("100"),
              new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(statusPublisher)
          .publishStatusUpdate(
              eq("specific-corr-id"),
              eq("order-456"),
              eq("trader1"),
              eq(OrderStatus.REJECTED),
              anyString());
    }

    @Test
    @DisplayName("Rejected order should not be forwarded to M7")
    void rejectedOrder_shouldNotBeForwardedToM7() {
      var invalidOrder =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              null,
              "BUY",
              new BigDecimal("100"),
              new BigDecimal("45.50"));

      orderConsumer.onOrderSubmit(invalidOrder);

      verify(m7Publisher, never()).publishOrder(any());
    }
  }

  @Nested
  @DisplayName("Data Integrity")
  class DataIntegrity {

    @Test
    @DisplayName("All order fields should be preserved in saved entity")
    void savedEntity_shouldPreserveAllFields() {
      var order =
          new OrderSubmitMessage(
              "corr-999",
              "order-888",
              "specificUser",
              Region.SOUTH,
              "SELL",
              new BigDecimal("250.5"),
              new BigDecimal("52.75"));

      orderConsumer.onOrderSubmit(order);

      ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
      verify(orderRepository).save(captor.capture());

      OrderEntity saved = captor.getValue();
      assertEquals("order-888", saved.getOrderId());
      assertEquals("corr-999", saved.getCorrelationId());
      assertEquals("specificUser", saved.getUsername());
      assertEquals(Region.SOUTH, saved.getRegion());
      assertEquals("SELL", saved.getOrderType());
      assertEquals(new BigDecimal("250.5"), saved.getQuantity());
      assertEquals(new BigDecimal("52.75"), saved.getPrice());
    }

    @Test
    @DisplayName("BigDecimal precision should be preserved")
    void bigDecimalPrecision_shouldBePreserved() {
      var order =
          new OrderSubmitMessage(
              "corr-123",
              "order-456",
              "trader1",
              Region.NORTH,
              "BUY",
              new BigDecimal("100.1234"),
              new BigDecimal("45.5678"));

      orderConsumer.onOrderSubmit(order);

      ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
      verify(orderRepository).save(captor.capture());

      assertEquals(new BigDecimal("100.1234"), captor.getValue().getQuantity());
      assertEquals(new BigDecimal("45.5678"), captor.getValue().getPrice());
    }
  }
}
