package com.trading.order.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.trading.common.OrderStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OrderEntity state machine.
 *
 * <p>Order lifecycle: PENDING → SUBMITTED → FILLED/REJECTED
 *
 * <p>These tests verify:
 *
 * <ul>
 *   <li>Valid state transitions work correctly
 *   <li>Invalid state transitions throw exceptions
 *   <li>Required fields are validated during transitions
 * </ul>
 */
class OrderEntityTest {

  private OrderEntity order;

  @BeforeEach
  void setUp() {
    order =
        new OrderEntity(
            "order-123",
            "corr-456",
            "trader1",
            "NORTH",
            "BUY",
            new BigDecimal("100"),
            new BigDecimal("45.50"));
  }

  @Nested
  @DisplayName("Initial State")
  class InitialState {

    @Test
    @DisplayName("New order should have PENDING status")
    void newOrder_shouldBePending() {
      assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    @DisplayName("New order should not have M7 reference ID")
    void newOrder_shouldNotHaveM7RefId() {
      assertNull(order.getM7ReferenceId());
    }

    @Test
    @DisplayName("New order should not have execution price")
    void newOrder_shouldNotHaveExecutionPrice() {
      assertNull(order.getExecutionPrice());
    }

    @Test
    @DisplayName("New order should not have reject reason")
    void newOrder_shouldNotHaveRejectReason() {
      assertNull(order.getRejectReason());
    }

    @Test
    @DisplayName("New order should preserve all input fields")
    void newOrder_shouldPreserveInputFields() {
      assertEquals("order-123", order.getOrderId());
      assertEquals("corr-456", order.getCorrelationId());
      assertEquals("trader1", order.getUsername());
      assertEquals("NORTH", order.getRegion());
      assertEquals("BUY", order.getOrderType());
      assertEquals(new BigDecimal("100"), order.getQuantity());
      assertEquals(new BigDecimal("45.50"), order.getPrice());
    }

    @Test
    @DisplayName("New order should have timestamps set")
    void newOrder_shouldHaveTimestamps() {
      assertNotNull(order.getCreatedAt());
      assertNotNull(order.getUpdatedAt());
    }
  }

  @Nested
  @DisplayName("PENDING → SUBMITTED Transition")
  class PendingToSubmitted {

    @Test
    @DisplayName("markSubmitted should change status to SUBMITTED")
    void markSubmitted_shouldChangeStatus() {
      order.markSubmitted("M7-REF-789");
      assertEquals(OrderStatus.SUBMITTED, order.getStatus());
    }

    @Test
    @DisplayName("markSubmitted should set M7 reference ID")
    void markSubmitted_shouldSetM7RefId() {
      order.markSubmitted("M7-REF-789");
      assertEquals("M7-REF-789", order.getM7ReferenceId());
    }

    @Test
    @DisplayName("markSubmitted should update timestamp")
    void markSubmitted_shouldUpdateTimestamp() {
      var beforeUpdate = order.getUpdatedAt();
      order.markSubmitted("M7-REF-789");
      assertTrue(
          order.getUpdatedAt().isAfter(beforeUpdate) || order.getUpdatedAt().equals(beforeUpdate));
    }

    @Test
    @DisplayName("markSubmitted with null reference should throw")
    void markSubmitted_withNullRef_shouldThrow() {
      var exception = assertThrows(IllegalArgumentException.class, () -> order.markSubmitted(null));
      assertEquals("M7 reference ID cannot be null or blank", exception.getMessage());
      assertEquals(OrderStatus.PENDING, order.getStatus()); // Status unchanged
    }

    @Test
    @DisplayName("markSubmitted with blank reference should throw")
    void markSubmitted_withBlankRef_shouldThrow() {
      var exception =
          assertThrows(IllegalArgumentException.class, () -> order.markSubmitted("   "));
      assertEquals("M7 reference ID cannot be null or blank", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("SUBMITTED → FILLED Transition")
  class SubmittedToFilled {

    @BeforeEach
    void setUpSubmitted() {
      order.markSubmitted("M7-REF-789");
    }

    @Test
    @DisplayName("markFilled should change status to FILLED")
    void markFilled_shouldChangeStatus() {
      order.markFilled(new BigDecimal("45.25"));
      assertEquals(OrderStatus.FILLED, order.getStatus());
    }

    @Test
    @DisplayName("markFilled should set execution price")
    void markFilled_shouldSetExecutionPrice() {
      order.markFilled(new BigDecimal("45.25"));
      assertEquals(new BigDecimal("45.25"), order.getExecutionPrice());
    }

    @Test
    @DisplayName("markFilled should preserve M7 reference ID")
    void markFilled_shouldPreserveM7RefId() {
      order.markFilled(new BigDecimal("45.25"));
      assertEquals("M7-REF-789", order.getM7ReferenceId());
    }

    @Test
    @DisplayName("markFilled with null price should throw")
    void markFilled_withNullPrice_shouldThrow() {
      var exception = assertThrows(IllegalArgumentException.class, () -> order.markFilled(null));
      assertEquals("Execution price cannot be null", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("SUBMITTED → REJECTED Transition")
  class SubmittedToRejected {

    @BeforeEach
    void setUpSubmitted() {
      order.markSubmitted("M7-REF-789");
    }

    @Test
    @DisplayName("markRejected should change status to REJECTED")
    void markRejected_shouldChangeStatus() {
      order.markRejected("Insufficient liquidity");
      assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    @DisplayName("markRejected should set reject reason")
    void markRejected_shouldSetReason() {
      order.markRejected("Insufficient liquidity");
      assertEquals("Insufficient liquidity", order.getRejectReason());
    }

    @Test
    @DisplayName("markRejected with null reason should still work")
    void markRejected_withNullReason_shouldWork() {
      // Null reason is allowed - sometimes rejection happens without a reason
      order.markRejected(null);
      assertEquals(OrderStatus.REJECTED, order.getStatus());
      assertNull(order.getRejectReason());
    }
  }

  @Nested
  @DisplayName("Invalid State Transitions")
  class InvalidTransitions {

    @Test
    @DisplayName("markFilled on PENDING order should throw")
    void markFilled_onPending_shouldThrow() {
      var exception =
          assertThrows(
              IllegalStateException.class, () -> order.markFilled(new BigDecimal("45.25")));
      assertTrue(exception.getMessage().contains("PENDING"));
      assertTrue(exception.getMessage().contains("expected SUBMITTED"));
    }

    @Test
    @DisplayName("markRejected on PENDING order should throw")
    void markRejected_onPending_shouldThrow() {
      var exception =
          assertThrows(IllegalStateException.class, () -> order.markRejected("Invalid"));
      assertTrue(exception.getMessage().contains("PENDING"));
      assertTrue(exception.getMessage().contains("expected SUBMITTED"));
    }

    @Test
    @DisplayName("markSubmitted on FILLED order should throw")
    void markSubmitted_onFilled_shouldThrow() {
      order.markSubmitted("M7-1");
      order.markFilled(new BigDecimal("45"));

      var exception = assertThrows(IllegalStateException.class, () -> order.markSubmitted("M7-2"));
      assertTrue(exception.getMessage().contains("FILLED"));
      assertTrue(exception.getMessage().contains("expected PENDING"));
    }

    @Test
    @DisplayName("markFilled on REJECTED order should throw")
    void markFilled_onRejected_shouldThrow() {
      order.markSubmitted("M7-1");
      order.markRejected("Too late");

      var exception =
          assertThrows(IllegalStateException.class, () -> order.markFilled(new BigDecimal("45")));
      assertTrue(exception.getMessage().contains("REJECTED"));
      assertTrue(exception.getMessage().contains("expected SUBMITTED"));
    }

    @Test
    @DisplayName("markFilled twice should throw")
    void markFilled_twice_shouldThrow() {
      order.markSubmitted("M7-1");
      order.markFilled(new BigDecimal("45.00"));

      var exception =
          assertThrows(
              IllegalStateException.class, () -> order.markFilled(new BigDecimal("50.00")));
      assertTrue(exception.getMessage().contains("FILLED"));
      assertTrue(exception.getMessage().contains("expected SUBMITTED"));
    }

    @Test
    @DisplayName("markSubmitted twice should throw")
    void markSubmitted_twice_shouldThrow() {
      order.markSubmitted("M7-1");

      var exception = assertThrows(IllegalStateException.class, () -> order.markSubmitted("M7-2"));
      assertTrue(exception.getMessage().contains("SUBMITTED"));
      assertTrue(exception.getMessage().contains("expected PENDING"));
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("Execution price can differ from requested price")
    void executionPrice_canDifferFromRequested() {
      order.markSubmitted("M7-1");
      order.markFilled(new BigDecimal("46.00")); // Different from 45.50

      assertNotEquals(order.getPrice(), order.getExecutionPrice());
      assertEquals(new BigDecimal("45.50"), order.getPrice());
      assertEquals(new BigDecimal("46.00"), order.getExecutionPrice());
    }

    @Test
    @DisplayName("BigDecimal precision is preserved")
    void bigDecimal_precisionPreserved() {
      order.markSubmitted("M7-1");
      order.markFilled(new BigDecimal("45.1234"));

      assertEquals(new BigDecimal("45.1234"), order.getExecutionPrice());
    }
  }
}
