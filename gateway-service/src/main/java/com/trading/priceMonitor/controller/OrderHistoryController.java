package com.trading.priceMonitor.controller;

import com.trading.priceMonitor.dto.OrderHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.security.Principal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/** REST controller for fetching order history. */
@RestController
@RequestMapping("/api/orders")
public class OrderHistoryController {

  private static final Logger log = LoggerFactory.getLogger(OrderHistoryController.class);

  private final RestTemplate restTemplate;
  private final String orderServiceUrl;

  public OrderHistoryController(
      RestTemplate restTemplate,
      @Value("${order-service.url:http://order-service:8081}") String orderServiceUrl) {
    this.restTemplate = restTemplate;
    this.orderServiceUrl = orderServiceUrl;
  }

  @Operation(
      summary = "Get order history",
      description = "Retrieve past orders for the authenticated user")
  @GetMapping("/history")
  public ResponseEntity<List<OrderHistoryResponse>> getOrderHistory(
      Principal principal, @RequestParam(defaultValue = "20") int limit) {

    String username = principal.getName();
    log.info("Fetching order history for user: {}, limit: {}", username, limit);

    String url = orderServiceUrl + "/api/orders/history/" + username + "?limit=" + limit;

    try {
      ResponseEntity<List<OrderHistoryResponse>> response =
          restTemplate.exchange(
              url,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<OrderHistoryResponse>>() {});

      List<OrderHistoryResponse> orders = response.getBody();
      log.info("Retrieved {} orders for user: {}", orders != null ? orders.size() : 0, username);
      return ResponseEntity.ok(orders != null ? orders : List.of());
    } catch (Exception e) {
      log.error("Failed to fetch order history from Order Service: {}", e.getMessage());
      return ResponseEntity.ok(List.of());
    }
  }
}
