package com.trading.priceMonitor.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trading.priceMonitor.entity.OrderEntity;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.model.Status;
import com.trading.priceMonitor.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderHistoryController.class)
class OrderHistoryControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private OrderService orderService;

  private UserEntity testUser;

  @BeforeEach
  void setUp() {
    testUser = new UserEntity("trader1", "hash");
  }

  @Test
  @WithMockUser(username = "trader1")
  void getUserOrders_shouldReturnOrderList() throws Exception {
    OrderEntity order1 = createOrder("order-1", Status.ACCEPTED);
    OrderEntity order2 = createOrder("order-2", Status.PENDING);

    when(orderService.findUserOrders("trader1")).thenReturn(List.of(order1, order2));

    mockMvc
        .perform(get("/api/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].orderId").value("order-1"))
        .andExpect(jsonPath("$[0].status").value("ACCEPTED"))
        .andExpect(jsonPath("$[1].orderId").value("order-2"))
        .andExpect(jsonPath("$[1].status").value("PENDING"));
  }

  @Test
  @WithMockUser(username = "trader1")
  void getUserOrders_shouldReturnEmptyListWhenNoOrders() throws Exception {
    when(orderService.findUserOrders("trader1")).thenReturn(List.of());

    mockMvc
        .perform(get("/api/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getUserOrders_shouldReturn401WhenNotAuthenticated() throws Exception {
    mockMvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "trader1")
  void getOrder_shouldReturnOrderWhenFound() throws Exception {
    OrderEntity order = createOrder("order-123", Status.ACCEPTED);

    when(orderService.findByOrderId("order-123")).thenReturn(Optional.of(order));

    mockMvc
        .perform(get("/api/orders/order-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value("order-123"))
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.region").value("DE"))
        .andExpect(jsonPath("$.orderType").value("BUY"));
  }

  @Test
  @WithMockUser(username = "trader1")
  void getOrder_shouldReturn404WhenNotFound() throws Exception {
    when(orderService.findByOrderId("nonexistent")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/orders/nonexistent")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "otherUser")
  void getOrder_shouldReturn404WhenOrderBelongsToAnotherUser() throws Exception {
    OrderEntity order = createOrder("order-123", Status.ACCEPTED);

    when(orderService.findByOrderId("order-123")).thenReturn(Optional.of(order));

    mockMvc.perform(get("/api/orders/order-123")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "trader1")
  void cancelOrder_shouldReturnCancelledOrder() throws Exception {
    OrderEntity order = createOrder("order-123", Status.CANCELLED);

    when(orderService.cancelOrder("order-123", "trader1")).thenReturn(Optional.of(order));

    mockMvc
        .perform(delete("/api/orders/order-123").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value("order-123"))
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  @WithMockUser(username = "trader1")
  void cancelOrder_shouldReturn404WhenOrderNotFound() throws Exception {
    when(orderService.cancelOrder("nonexistent", "trader1")).thenReturn(Optional.empty());

    mockMvc
        .perform(delete("/api/orders/nonexistent").with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "trader1")
  void cancelOrder_shouldReturn404WhenOrderCannotBeCancelled() throws Exception {
    when(orderService.cancelOrder("order-123", "trader1")).thenReturn(Optional.empty());

    mockMvc.perform(delete("/api/orders/order-123").with(csrf())).andExpect(status().isNotFound());
  }

  @Test
  void cancelOrder_shouldReturn401WhenNotAuthenticated() throws Exception {
    mockMvc
        .perform(delete("/api/orders/order-123").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  private OrderEntity createOrder(String orderId, Status status) {
    OrderEntity order =
        new OrderEntity(
            orderId, testUser, "DE", "BUY", new BigDecimal("100"), new BigDecimal("50.00"));
    order.setStatus(status);
    return order;
  }
}
