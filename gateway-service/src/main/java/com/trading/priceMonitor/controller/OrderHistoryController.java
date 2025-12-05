package com.trading.priceMonitor.controller;

import com.trading.priceMonitor.dto.OrderResponse;
import com.trading.priceMonitor.service.OrderService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderHistoryController {

  private final OrderService orderService;

  public OrderHistoryController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping
  public ResponseEntity<List<OrderResponse>> getUserOrders(Principal principal) {
    if (principal == null) {
      return ResponseEntity.status(401).build();
    }

    List<OrderResponse> orders =
        orderService.findUserOrders(principal.getName()).stream().map(OrderResponse::from).toList();

    return ResponseEntity.ok(orders);
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId, Principal principal) {
    if (principal == null) {
      return ResponseEntity.status(401).build();
    }

    return orderService
        .findByOrderId(orderId)
        .filter(order -> order.getUser().getUsername().equals(principal.getName()))
        .map(OrderResponse::from)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{orderId}")
  public ResponseEntity<OrderResponse> cancelOrder(
      @PathVariable String orderId, Principal principal) {
    if (principal == null) {
      return ResponseEntity.status(401).build();
    }

    return orderService
        .cancelOrder(orderId, principal.getName())
        .map(OrderResponse::from)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
