package com.trading.priceMonitor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.trading.priceMonitor.dto.OrderMessage;
import com.trading.priceMonitor.entity.OrderEntity;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.model.Status;
import com.trading.priceMonitor.repository.OrderRepository;
import com.trading.priceMonitor.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;
  @Mock private UserRepository userRepository;
  @InjectMocks private OrderService orderService;

  private UserEntity testUser;
  private OrderMessage validOrderMessage;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity("trader1", "hashedPassword");

    validOrderMessage =
        new OrderMessage(
            "order-123", "trader1", "DE", "BUY", new BigDecimal("100"), new BigDecimal("50.00"));
  }

  @Test
  void processOrder_shouldAcceptValidOrder() {
    when(orderRepository.existsByOrderId("order-123")).thenReturn(false);
    when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.processOrder(validOrderMessage);

    assertTrue(result.isPresent());
    assertEquals(Status.ACCEPTED, result.get().getStatus());
    assertEquals("order-123", result.get().getOrderId());
    verify(orderRepository).save(any(OrderEntity.class));
  }

  @Test
  void processOrder_shouldRejectDuplicateOrder() {
    when(orderRepository.existsByOrderId("order-123")).thenReturn(true);

    Optional<OrderEntity> result = orderService.processOrder(validOrderMessage);

    assertTrue(result.isEmpty());
    verify(orderRepository, never()).save(any());
  }

  @Test
  void processOrder_shouldRejectOrderWithInvalidQuantity() {
    OrderMessage invalidOrder =
        new OrderMessage(
            "order-456",
            "trader1",
            "DE",
            "BUY",
            new BigDecimal("-10"), // negative quantity
            new BigDecimal("50.00"));

    when(orderRepository.existsByOrderId("order-456")).thenReturn(false);
    when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.processOrder(invalidOrder);

    assertTrue(result.isPresent());
    assertEquals(Status.REJECTED, result.get().getStatus());
  }

  @Test
  void processOrder_shouldRejectOrderWithInvalidPrice() {
    OrderMessage invalidOrder =
        new OrderMessage(
            "order-789",
            "trader1",
            "DE",
            "BUY",
            new BigDecimal("100"),
            new BigDecimal("0")); // zero price

    when(orderRepository.existsByOrderId("order-789")).thenReturn(false);
    when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.processOrder(invalidOrder);

    assertTrue(result.isPresent());
    assertEquals(Status.REJECTED, result.get().getStatus());
  }

  @Test
  void processOrder_shouldReturnEmptyWhenUserNotFound() {
    when(orderRepository.existsByOrderId("order-123")).thenReturn(false);
    when(userRepository.findByUsername("trader1")).thenReturn(Optional.empty());

    Optional<OrderEntity> result = orderService.processOrder(validOrderMessage);

    assertTrue(result.isEmpty());
    verify(orderRepository, never()).save(any());
  }

  @Test
  void findUserOrders_shouldReturnOrdersForUser() {
    OrderEntity order1 = createOrder("order-1", Status.ACCEPTED);
    OrderEntity order2 = createOrder("order-2", Status.PENDING);

    when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(orderRepository.findByUserOrderByCreatedAtDesc(testUser))
        .thenReturn(List.of(order1, order2));

    List<OrderEntity> orders = orderService.findUserOrders("trader1");

    assertEquals(2, orders.size());
  }

  @Test
  void findUserOrders_shouldReturnEmptyListWhenUserNotFound() {
    when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

    List<OrderEntity> orders = orderService.findUserOrders("unknown");

    assertTrue(orders.isEmpty());
  }

  @Test
  void cancelOrder_shouldCancelAcceptedOrder() {
    OrderEntity order = createOrder("order-123", Status.ACCEPTED);

    when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(order));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.cancelOrder("order-123", "trader1");

    assertTrue(result.isPresent());
    assertEquals(Status.CANCELLED, result.get().getStatus());
  }

  @Test
  void cancelOrder_shouldCancelPendingOrder() {
    OrderEntity order = createOrder("order-123", Status.PENDING);

    when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(order));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.cancelOrder("order-123", "trader1");

    assertTrue(result.isPresent());
    assertEquals(Status.CANCELLED, result.get().getStatus());
  }

  @Test
  void cancelOrder_shouldNotCancelRejectedOrder() {
    OrderEntity order = createOrder("order-123", Status.REJECTED);

    when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(order));

    Optional<OrderEntity> result = orderService.cancelOrder("order-123", "trader1");

    assertTrue(result.isEmpty());
    verify(orderRepository, never()).save(any());
  }

  @Test
  void cancelOrder_shouldNotCancelAlreadyCancelledOrder() {
    OrderEntity order = createOrder("order-123", Status.CANCELLED);

    when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(order));

    Optional<OrderEntity> result = orderService.cancelOrder("order-123", "trader1");

    assertTrue(result.isEmpty());
    verify(orderRepository, never()).save(any());
  }

  @Test
  void cancelOrder_shouldNotAllowCancellingOtherUsersOrder() {
    UserEntity otherUser = new UserEntity("otherTrader", "hash");
    OrderEntity order =
        new OrderEntity(
            "order-123", otherUser, "DE", "BUY", new BigDecimal("100"), new BigDecimal("50.00"));
    order.setStatus(Status.ACCEPTED);

    when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(order));

    Optional<OrderEntity> result = orderService.cancelOrder("order-123", "trader1");

    assertTrue(result.isEmpty());
    verify(orderRepository, never()).save(any());
  }

  @Test
  void cancelOrder_shouldReturnEmptyWhenOrderNotFound() {
    when(orderRepository.findByOrderId("nonexistent")).thenReturn(Optional.empty());

    Optional<OrderEntity> result = orderService.cancelOrder("nonexistent", "trader1");

    assertTrue(result.isEmpty());
  }

  @Test
  void processOrder_shouldRejectOrderWithZeroQuantity() {
    OrderMessage zeroQty =
        new OrderMessage(
            "order-zero", "trader1", "DE", "BUY", BigDecimal.ZERO, new BigDecimal("50.00"));

    when(orderRepository.existsByOrderId("order-zero")).thenReturn(false);
    when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.processOrder(zeroQty);

    assertTrue(result.isPresent());
    assertEquals(Status.REJECTED, result.get().getStatus());
  }

  @Test
  void processOrder_shouldRejectOrderWithNegativePrice() {
    OrderMessage negativePrice =
        new OrderMessage(
            "order-negprice", "trader1", "DE", "SELL", new BigDecimal("100"), new BigDecimal("-1"));

    when(orderRepository.existsByOrderId("order-negprice")).thenReturn(false);
    when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.processOrder(negativePrice);

    assertTrue(result.isPresent());
    assertEquals(Status.REJECTED, result.get().getStatus());
  }

  @Test
  void processOrder_shouldAcceptSellOrder() {
    OrderMessage sellOrder =
        new OrderMessage(
            "sell-order", "trader1", "DE", "SELL", new BigDecimal("50"), new BigDecimal("75.50"));

    when(orderRepository.existsByOrderId("sell-order")).thenReturn(false);
    when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.processOrder(sellOrder);

    assertTrue(result.isPresent());
    assertEquals(Status.ACCEPTED, result.get().getStatus());
    assertEquals("SELL", result.get().getOrderType());
  }

  @Test
  void processOrder_shouldPreserveOrderDetails() {
    OrderMessage order =
        new OrderMessage(
            "detailed-order",
            "trader1",
            "FR",
            "BUY",
            new BigDecimal("250"),
            new BigDecimal("99.99"));

    when(orderRepository.existsByOrderId("detailed-order")).thenReturn(false);
    when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
    when(orderRepository.save(any(OrderEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<OrderEntity> result = orderService.processOrder(order);

    assertTrue(result.isPresent());
    OrderEntity saved = result.get();
    assertEquals("detailed-order", saved.getOrderId());
    assertEquals("FR", saved.getRegion());
    assertEquals("BUY", saved.getOrderType());
    assertEquals(new BigDecimal("250"), saved.getQuantity());
    assertEquals(new BigDecimal("99.99"), saved.getPrice());
    assertEquals(testUser, saved.getUser());
  }

  @Test
  void findByOrderId_shouldReturnOrderWhenExists() {
    OrderEntity order = createOrder("order-123", Status.ACCEPTED);
    when(orderRepository.findByOrderId("order-123")).thenReturn(Optional.of(order));

    Optional<OrderEntity> result = orderService.findByOrderId("order-123");

    assertTrue(result.isPresent());
    assertEquals("order-123", result.get().getOrderId());
  }

  @Test
  void findByOrderId_shouldReturnEmptyWhenNotExists() {
    when(orderRepository.findByOrderId("nonexistent")).thenReturn(Optional.empty());

    Optional<OrderEntity> result = orderService.findByOrderId("nonexistent");

    assertTrue(result.isEmpty());
  }

  // Uses testUser which has username "trader1"
  private OrderEntity createOrder(String orderId, Status status) {
    OrderEntity order =
        new OrderEntity(
            orderId, testUser, "DE", "BUY", new BigDecimal("100"), new BigDecimal("50.00"));
    order.setStatus(status);
    return order;
  }
}
