package com.trading.priceMonitor.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.trading.priceMonitor.dto.OrderMessage;
import com.trading.priceMonitor.entity.OrderEntity;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.model.OrderConfirmation;
import com.trading.priceMonitor.model.Status;
import com.trading.priceMonitor.service.OrderService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

  @Mock private OrderService orderService;
  @Mock private SimpMessagingTemplate messagingTemplate;
  @InjectMocks private OrderConsumer orderConsumer;

  private UserEntity testUser;
  private OrderMessage validMessage;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity("trader1", "hash");
    validMessage =
        new OrderMessage(
            "order-123", "trader1", "DE", "BUY", new BigDecimal("100"), new BigDecimal("50.00"));
  }

  @Test
  void consume_shouldSendAcceptedConfirmationForValidOrder() {
    OrderEntity acceptedOrder = createOrder("order-123", Status.ACCEPTED);
    when(orderService.processOrder(validMessage)).thenReturn(Optional.of(acceptedOrder));

    orderConsumer.consume(validMessage);

    ArgumentCaptor<OrderConfirmation> captor = ArgumentCaptor.forClass(OrderConfirmation.class);
    verify(messagingTemplate)
        .convertAndSendToUser(eq("trader1"), eq("/queue/order-confirmation"), captor.capture());

    OrderConfirmation confirmation = captor.getValue();
    assertEquals("order-123", confirmation.orderId());
    assertEquals(Status.ACCEPTED, confirmation.status());
    assertEquals("Order accepted and persisted", confirmation.message());
  }

  @Test
  void consume_shouldSendRejectedConfirmationForInvalidOrder() {
    OrderEntity rejectedOrder = createOrder("order-123", Status.REJECTED);
    when(orderService.processOrder(validMessage)).thenReturn(Optional.of(rejectedOrder));

    orderConsumer.consume(validMessage);

    ArgumentCaptor<OrderConfirmation> captor = ArgumentCaptor.forClass(OrderConfirmation.class);
    verify(messagingTemplate)
        .convertAndSendToUser(eq("trader1"), eq("/queue/order-confirmation"), captor.capture());

    OrderConfirmation confirmation = captor.getValue();
    assertEquals("order-123", confirmation.orderId());
    assertEquals(Status.REJECTED, confirmation.status());
    assertEquals("Order rejected: validation failed", confirmation.message());
  }

  @Test
  void consume_shouldSendFailedConfirmationWhenProcessingFails() {
    when(orderService.processOrder(validMessage)).thenReturn(Optional.empty());

    orderConsumer.consume(validMessage);

    ArgumentCaptor<OrderConfirmation> captor = ArgumentCaptor.forClass(OrderConfirmation.class);
    verify(messagingTemplate)
        .convertAndSendToUser(eq("trader1"), eq("/queue/order-confirmation"), captor.capture());

    OrderConfirmation confirmation = captor.getValue();
    assertEquals("order-123", confirmation.orderId());
    assertEquals(Status.REJECTED, confirmation.status());
    assertEquals("Order processing failed", confirmation.message());
  }

  @Test
  void consume_shouldNotSendConfirmationForBlankUsername() {
    OrderMessage blankUsernameMessage =
        new OrderMessage(
            "order-123", "", "DE", "BUY", new BigDecimal("100"), new BigDecimal("50.00"));
    when(orderService.processOrder(blankUsernameMessage)).thenReturn(Optional.empty());

    orderConsumer.consume(blankUsernameMessage);

    verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
  }

  @Test
  void consume_shouldNotSendConfirmationForNullUsername() {
    OrderMessage nullUsernameMessage =
        new OrderMessage(
            "order-123", null, "DE", "BUY", new BigDecimal("100"), new BigDecimal("50.00"));
    when(orderService.processOrder(nullUsernameMessage)).thenReturn(Optional.empty());

    orderConsumer.consume(nullUsernameMessage);

    verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
  }

  private OrderEntity createOrder(String orderId, Status status) {
    OrderEntity order =
        new OrderEntity(
            orderId, testUser, "DE", "BUY", new BigDecimal("100"), new BigDecimal("50.00"));
    order.setStatus(status);
    return order;
  }
}
