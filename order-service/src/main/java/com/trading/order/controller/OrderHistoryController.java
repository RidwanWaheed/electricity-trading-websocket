package com.trading.order.controller;

import com.trading.order.dto.OrderHistoryResponse;
import com.trading.order.entity.OrderEntity;
import com.trading.order.repository.OrderRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for order history queries.
 *
 * <p>This endpoint is called by Gateway to fetch a user's order history.
 */
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderHistoryController {

  private static final Logger log = LoggerFactory.getLogger(OrderHistoryController.class);

  private final OrderRepository orderRepository;

  public OrderHistoryController(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  /**
   * Get order history for a specific user.
   *
   * @param username The username to fetch orders for
   * @param limit Maximum number of orders to return (1-100, default 20)
   * @return List of orders, most recent first
   */
  @GetMapping("/history/{username}")
  public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(
      @PathVariable String username,
      @RequestParam(defaultValue = "20")
          @Min(value = 1, message = "Limit must be at least 1")
          @Max(value = 100, message = "Limit cannot exceed 100")
          int limit) {

    log.info("Fetching order history for user: {}, limit: {}", username, limit);

    List<OrderEntity> orders = orderRepository.findByUsernameOrderByCreatedAtDesc(username);

    // Apply limit
    if (orders.size() > limit) {
      orders = orders.subList(0, limit);
    }

    List<OrderHistoryResponse> response =
        orders.stream()
            .map(
                order ->
                    new OrderHistoryResponse(
                        order.getOrderId(),
                        order.getOrderType(),
                        order.getRegion(),
                        order.getQuantity().toString(),
                        order.getPrice().toString(),
                        order.getStatus().name(),
                        order.getRejectReason(),
                        order.getCreatedAt().toString()))
            .toList();

    log.info("Returning {} orders for user: {}", response.size(), username);
    return ResponseEntity.ok(response);
  }
}
