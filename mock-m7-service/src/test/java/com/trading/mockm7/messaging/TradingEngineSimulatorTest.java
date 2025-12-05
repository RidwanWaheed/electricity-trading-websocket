package com.trading.mockm7.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.trading.common.messaging.M7AckResponse;
import com.trading.common.messaging.M7OrderRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for TradingEngineSimulator.
 *
 * <p>Tests the two-phase response pattern:
 *
 * <ul>
 *   <li>ACK is sent immediately when order is received
 *   <li>FILL/REJECT is sent asynchronously after delay
 *   <li>Correlation ID is preserved through all responses
 *   <li>M7 reference IDs are unique per order
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TradingEngineSimulatorTest {

  @Mock private M7ResponsePublisher responsePublisher;

  private TradingEngineSimulator simulator;

  @BeforeEach
  void setUp() {
    simulator = new TradingEngineSimulator(responsePublisher);
  }

  private M7OrderRequest createOrder(String corrId, String orderId) {
    return new M7OrderRequest(
        corrId, orderId, "NORTH", "BUY", new BigDecimal("100"), new BigDecimal("45.50"));
  }

  @Nested
  @DisplayName("ACK Response")
  class AckResponse {

    @Test
    @DisplayName("ACK should be sent immediately when order is processed")
    void processOrder_shouldSendImmediateAck() {
      var order = createOrder("corr-123", "order-456");

      simulator.processOrder(order);

      verify(responsePublisher).publishAck(any(M7AckResponse.class));
    }

    @Test
    @DisplayName("ACK should preserve correlation ID from request")
    void ack_shouldPreserveCorrelationId() {
      var order = createOrder("unique-corr-id", "order-456");

      simulator.processOrder(order);

      ArgumentCaptor<M7AckResponse> captor = ArgumentCaptor.forClass(M7AckResponse.class);
      verify(responsePublisher).publishAck(captor.capture());

      assertEquals("unique-corr-id", captor.getValue().correlationId());
    }

    @Test
    @DisplayName("ACK should preserve order ID from request")
    void ack_shouldPreserveOrderId() {
      var order = createOrder("corr-123", "specific-order-id");

      simulator.processOrder(order);

      ArgumentCaptor<M7AckResponse> captor = ArgumentCaptor.forClass(M7AckResponse.class);
      verify(responsePublisher).publishAck(captor.capture());

      assertEquals("specific-order-id", captor.getValue().orderId());
    }

    @Test
    @DisplayName("ACK should include M7 reference ID starting with 'M7-'")
    void ack_shouldGenerateM7ReferenceId() {
      var order = createOrder("corr-123", "order-456");

      simulator.processOrder(order);

      ArgumentCaptor<M7AckResponse> captor = ArgumentCaptor.forClass(M7AckResponse.class);
      verify(responsePublisher).publishAck(captor.capture());

      String refId = captor.getValue().m7ReferenceId();
      assertNotNull(refId);
      assertTrue(refId.startsWith("M7-"), "M7 reference ID should start with 'M7-'");
    }

    @Test
    @DisplayName("Each order should get unique M7 reference ID")
    void multipleOrders_shouldGetUniqueM7ReferenceIds() {
      var order1 = createOrder("corr-1", "order-1");
      var order2 = createOrder("corr-2", "order-2");

      simulator.processOrder(order1);
      simulator.processOrder(order2);

      ArgumentCaptor<M7AckResponse> captor = ArgumentCaptor.forClass(M7AckResponse.class);
      verify(responsePublisher, times(2)).publishAck(captor.capture());

      String refId1 = captor.getAllValues().get(0).m7ReferenceId();
      String refId2 = captor.getAllValues().get(1).m7ReferenceId();

      assertNotEquals(refId1, refId2, "M7 reference IDs should be unique");
    }

    @Test
    @DisplayName("ACK should have timestamp set")
    void ack_shouldHaveTimestamp() {
      var order = createOrder("corr-123", "order-456");

      simulator.processOrder(order);

      ArgumentCaptor<M7AckResponse> captor = ArgumentCaptor.forClass(M7AckResponse.class);
      verify(responsePublisher).publishAck(captor.capture());

      assertNotNull(captor.getValue().timestamp(), "ACK should have a timestamp");
    }
  }

  @Nested
  @DisplayName("Order Processing")
  class OrderProcessing {

    @Test
    @DisplayName("BUY orders should be processed")
    void buyOrder_shouldBeProcessed() {
      var buyOrder =
          new M7OrderRequest(
              "corr-1", "order-1", "NORTH", "BUY", new BigDecimal("100"), new BigDecimal("45"));

      simulator.processOrder(buyOrder);

      verify(responsePublisher).publishAck(any());
    }

    @Test
    @DisplayName("SELL orders should be processed")
    void sellOrder_shouldBeProcessed() {
      var sellOrder =
          new M7OrderRequest(
              "corr-1", "order-1", "SOUTH", "SELL", new BigDecimal("50"), new BigDecimal("46"));

      simulator.processOrder(sellOrder);

      verify(responsePublisher).publishAck(any());
    }

    @Test
    @DisplayName("Different regions should be accepted")
    void differentRegions_shouldBeAccepted() {
      var northOrder =
          new M7OrderRequest(
              "corr-1", "order-1", "NORTH", "BUY", new BigDecimal("100"), new BigDecimal("45"));
      var southOrder =
          new M7OrderRequest(
              "corr-2", "order-2", "SOUTH", "BUY", new BigDecimal("100"), new BigDecimal("45"));

      simulator.processOrder(northOrder);
      simulator.processOrder(southOrder);

      verify(responsePublisher, times(2)).publishAck(any());
    }
  }

  @Nested
  @DisplayName("Data Integrity")
  class DataIntegrity {

    @Test
    @DisplayName("Large quantities should be handled correctly")
    void largeQuantity_shouldBeProcessed() {
      var largeOrder =
          new M7OrderRequest(
              "corr-1",
              "order-1",
              "NORTH",
              "BUY",
              new BigDecimal("999999.9999"),
              new BigDecimal("45"));

      simulator.processOrder(largeOrder);

      verify(responsePublisher).publishAck(any());
    }

    @Test
    @DisplayName("High precision prices should be handled correctly")
    void precisePrice_shouldBeProcessed() {
      var preciseOrder =
          new M7OrderRequest(
              "corr-1",
              "order-1",
              "NORTH",
              "BUY",
              new BigDecimal("100"),
              new BigDecimal("45.123456"));

      simulator.processOrder(preciseOrder);

      verify(responsePublisher).publishAck(any());
    }
  }
}
