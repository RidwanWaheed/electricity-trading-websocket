package com.trading.order.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trading.common.OrderStatus;
import com.trading.common.Region;
import com.trading.common.messaging.M7AckResponse;
import com.trading.common.messaging.M7FillResponse;
import com.trading.order.entity.OrderEntity;
import com.trading.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for M7ResponseListener.
 *
 * <p>Tests the handling of M7 responses:
 *
 * <ul>
 *   <li>ACK updates order to SUBMITTED
 *   <li>FILL updates order to FILLED with execution price
 *   <li>REJECT updates order to REJECTED with reason
 *   <li>Duplicate/out-of-order messages are handled gracefully (idempotent)
 *   <li>Unknown order IDs are logged but don't cause errors
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class M7ResponseListenerTest {

  @Mock private OrderRepository orderRepository;
  @Mock private StatusPublisher statusPublisher;
  @InjectMocks private M7ResponseListener listener;

  private OrderEntity testOrder;

  @BeforeEach
  void setUp() {
    testOrder =
        new OrderEntity(
            "order-123",
            "corr-456",
            "trader1",
            Region.NORTH,
            "BUY",
            new BigDecimal("100"),
            new BigDecimal("45.50"));
  }

  @Nested
  @DisplayName("ACK Response Handling")
  class AckResponseHandling {

    @Test
    @DisplayName("ACK should update order to SUBMITTED")
    void onM7Ack_shouldUpdateOrderToSubmitted() {
      M7AckResponse ack = new M7AckResponse("corr-456", "order-123", "M7-REF-789", Instant.now());
      when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(testOrder));

      listener.onM7Ack(ack);

      verify(orderRepository).save(testOrder);
      assertEquals(OrderStatus.SUBMITTED, testOrder.getStatus());
      assertEquals("M7-REF-789", testOrder.getM7ReferenceId());

      verify(statusPublisher)
          .publishStatusUpdate(
              eq("corr-456"),
              eq("order-123"),
              eq("trader1"),
              eq(OrderStatus.SUBMITTED),
              anyString());
    }

    @Test
    @DisplayName("ACK for unknown order should not throw")
    void onM7Ack_shouldHandleUnknownOrder() {
      M7AckResponse ack =
          new M7AckResponse("corr-456", "unknown-order", "M7-REF-789", Instant.now());
      when(orderRepository.findByOrderId("unknown-order")).thenReturn(Optional.empty());

      // Should not throw, just log error
      assertDoesNotThrow(() -> listener.onM7Ack(ack));

      verify(orderRepository, never()).save(any());
      verify(statusPublisher, never()).publishStatusUpdate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Duplicate ACK should be ignored (idempotent)")
    void onM7Ack_duplicateAck_shouldBeIgnored() {
      // First ACK - transitions PENDING → SUBMITTED
      testOrder.markSubmitted("M7-REF-789");

      M7AckResponse duplicateAck =
          new M7AckResponse("corr-456", "order-123", "M7-REF-789", Instant.now());
      when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(testOrder));

      // Second ACK should not throw, just be ignored
      assertDoesNotThrow(() -> listener.onM7Ack(duplicateAck));

      // Should NOT save (no state change)
      verify(orderRepository, never()).save(any());
      // Should NOT publish status (no state change)
      verify(statusPublisher, never()).publishStatusUpdate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("ACK for already FILLED order should be ignored")
    void onM7Ack_alreadyFilled_shouldBeIgnored() {
      testOrder.markSubmitted("M7-REF-789");
      testOrder.markFilled(new BigDecimal("45.00"));

      M7AckResponse lateAck =
          new M7AckResponse("corr-456", "order-123", "M7-REF-NEW", Instant.now());
      when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(testOrder));

      assertDoesNotThrow(() -> listener.onM7Ack(lateAck));

      verify(orderRepository, never()).save(any());
      // Status should still be FILLED
      assertEquals(OrderStatus.FILLED, testOrder.getStatus());
    }
  }

  @Nested
  @DisplayName("FILL Response Handling")
  class FillResponseHandling {

    @Test
    @DisplayName("FILL should update order to FILLED")
    void onM7Fill_shouldUpdateOrderToFilled() {
      testOrder.markSubmitted("M7-REF-789");

      M7FillResponse fill = M7FillResponse.filled("corr-456", "order-123", new BigDecimal("45.25"));
      when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(testOrder));

      listener.onM7Fill(fill);

      verify(orderRepository).save(testOrder);
      assertEquals(OrderStatus.FILLED, testOrder.getStatus());
      assertEquals(new BigDecimal("45.25"), testOrder.getExecutionPrice());

      verify(statusPublisher)
          .publishStatusUpdate(
              eq("corr-456"), eq("order-123"), eq("trader1"), eq(OrderStatus.FILLED), anyString());
    }

    @Test
    @DisplayName("REJECT should update order to REJECTED")
    void onM7Fill_shouldUpdateOrderToRejected() {
      testOrder.markSubmitted("M7-REF-789");

      M7FillResponse reject =
          M7FillResponse.rejected("corr-456", "order-123", "Price out of range");
      when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(testOrder));

      listener.onM7Fill(reject);

      verify(orderRepository).save(testOrder);
      assertEquals(OrderStatus.REJECTED, testOrder.getStatus());
      assertEquals("Price out of range", testOrder.getRejectReason());

      verify(statusPublisher)
          .publishStatusUpdate(
              eq("corr-456"),
              eq("order-123"),
              eq("trader1"),
              eq(OrderStatus.REJECTED),
              anyString());
    }

    @Test
    @DisplayName("FILL for unknown order should not throw")
    void onM7Fill_shouldHandleUnknownOrder() {
      M7FillResponse fill =
          M7FillResponse.filled("corr-456", "unknown-order", new BigDecimal("45"));
      when(orderRepository.findByOrderId("unknown-order")).thenReturn(Optional.empty());

      assertDoesNotThrow(() -> listener.onM7Fill(fill));

      verify(orderRepository, never()).save(any());
      verify(statusPublisher, never()).publishStatusUpdate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Duplicate FILL should be ignored (idempotent)")
    void onM7Fill_duplicateFill_shouldBeIgnored() {
      testOrder.markSubmitted("M7-REF-789");
      testOrder.markFilled(new BigDecimal("45.00"));

      M7FillResponse duplicateFill =
          M7FillResponse.filled("corr-456", "order-123", new BigDecimal("46.00"));
      when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(testOrder));

      assertDoesNotThrow(() -> listener.onM7Fill(duplicateFill));

      verify(orderRepository, never()).save(any());
      // Original execution price should be preserved
      assertEquals(new BigDecimal("45.00"), testOrder.getExecutionPrice());
    }

    @Test
    @DisplayName("FILL on PENDING order should be ignored (out of order)")
    void onM7Fill_onPendingOrder_shouldBeIgnored() {
      // Order is still PENDING (no ACK received yet)
      M7FillResponse fill = M7FillResponse.filled("corr-456", "order-123", new BigDecimal("45.00"));
      when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(testOrder));

      assertDoesNotThrow(() -> listener.onM7Fill(fill));

      verify(orderRepository, never()).save(any());
      assertEquals(OrderStatus.PENDING, testOrder.getStatus());
    }

    @Test
    @DisplayName("REJECT after FILL should be ignored")
    void onM7Fill_rejectAfterFill_shouldBeIgnored() {
      testOrder.markSubmitted("M7-REF-789");
      testOrder.markFilled(new BigDecimal("45.00"));

      M7FillResponse lateReject = M7FillResponse.rejected("corr-456", "order-123", "Too late");
      when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(testOrder));

      assertDoesNotThrow(() -> listener.onM7Fill(lateReject));

      verify(orderRepository, never()).save(any());
      assertEquals(OrderStatus.FILLED, testOrder.getStatus());
      assertNull(testOrder.getRejectReason());
    }
  }
}
