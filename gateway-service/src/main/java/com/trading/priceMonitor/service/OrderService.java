package com.trading.priceMonitor.service;

import com.trading.priceMonitor.dto.OrderMessage;
import com.trading.priceMonitor.entity.OrderEntity;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.model.Status;
import com.trading.priceMonitor.repository.OrderRepository;
import com.trading.priceMonitor.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

  private static final Logger log = LoggerFactory.getLogger(OrderService.class);

  private final OrderRepository orderRepository;
  private final UserRepository userRepository;

  public OrderService(OrderRepository orderRepository, UserRepository userRepository) {
    this.orderRepository = orderRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public Optional<OrderEntity> processOrder(OrderMessage message) {
    log.info("Processing order: orderId={}, user={}", message.orderId(), message.username());

    if (orderRepository.existsByOrderId(message.orderId())) {
      log.warn("Duplicate order rejected: {}", message.orderId());
      return Optional.empty();
    }

    Optional<UserEntity> userOptional = userRepository.findByUsername(message.username());
    if (userOptional.isEmpty()) {
      log.error(
          "User not found for order: user={}, orderId={}", message.username(), message.orderId());
      return Optional.empty();
    }

    UserEntity user = userOptional.get();

    OrderEntity order =
        new OrderEntity(
            message.orderId(),
            user,
            message.region(),
            message.orderType(),
            message.quantity(),
            message.price());

    if (isValidOrder(message)) {
      order.setStatus(Status.ACCEPTED);
    } else {
      order.setStatus(Status.REJECTED);
    }

    OrderEntity savedOrder = orderRepository.save(order);
    log.info(
        "Order processed: orderId={}, status={}", savedOrder.getOrderId(), savedOrder.getStatus());

    return Optional.of(savedOrder);
  }

  private boolean isValidOrder(OrderMessage message) {
    return message.quantity().signum() > 0 && message.price().signum() > 0;
  }

  public List<OrderEntity> findUserOrders(String username) {
    return userRepository
        .findByUsername(username)
        .map(orderRepository::findByUserOrderByCreatedAtDesc)
        .orElse(List.of());
  }

  public Optional<OrderEntity> findByOrderId(String orderId) {
    return orderRepository.findByOrderId(orderId);
  }

  // Only PENDING or ACCEPTED orders can be cancelled (not already REJECTED/CANCELLED)
  @Transactional
  public Optional<OrderEntity> cancelOrder(String orderId, String username) {
    return orderRepository
        .findByOrderId(orderId)
        .filter(order -> order.getUser().getUsername().equals(username))
        .filter(
            order -> order.getStatus() == Status.PENDING || order.getStatus() == Status.ACCEPTED)
        .map(
            order -> {
              order.setStatus(Status.CANCELLED);
              log.info("Order cancelled: orderId={}, user={}", orderId, username);
              return orderRepository.save(order);
            });
  }
}
