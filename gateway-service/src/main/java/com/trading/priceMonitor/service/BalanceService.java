package com.trading.priceMonitor.service;

import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user balances.
 *
 * <p>Handles balance checks for order submissions and balance updates on order fills.
 *
 * <p>Tracks reserved amounts for pending BUY orders so they can be refunded if rejected.
 */
@Service
public class BalanceService {

  private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

  private final UserRepository userRepository;

  /**
   * Tracks reserved amounts for pending orders.
   *
   * <p>Key: orderId, Value: ReservedAmount (username + amount)
   */
  private final ConcurrentHashMap<String, ReservedAmount> reservedAmounts = new ConcurrentHashMap<>();

  public BalanceService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** Holds information about a reserved amount for an order. */
  private record ReservedAmount(String username, BigDecimal amount) {}

  /**
   * Get the current balance for a user.
   *
   * @param username The username
   * @return The balance, or empty if user not found
   */
  public Optional<BigDecimal> getBalance(String username) {
    return userRepository.findByUsername(username).map(UserEntity::getBalance);
  }

  /**
   * Check if user has sufficient balance for a BUY order.
   *
   * @param username The username
   * @param orderCost The total cost (price * quantity)
   * @return true if sufficient balance
   */
  public boolean hasSufficientBalance(String username, BigDecimal orderCost) {
    return userRepository
        .findByUsername(username)
        .map(user -> user.getBalance().compareTo(orderCost) >= 0)
        .orElse(false);
  }

  /**
   * Reserve balance for a BUY order (deduct from available).
   *
   * @param orderId The order ID (for tracking the reservation)
   * @param username The username
   * @param amount The amount to reserve
   * @return true if reservation successful
   */
  @Transactional
  public boolean reserveBalance(String orderId, String username, BigDecimal amount) {
    Optional<UserEntity> userOpt = userRepository.findByUsername(username);
    if (userOpt.isEmpty()) {
      log.warn("User not found for balance reservation: {}", username);
      return false;
    }

    UserEntity user = userOpt.get();
    if (user.deductBalance(amount)) {
      userRepository.save(user);
      // Track the reservation for potential refund
      reservedAmounts.put(orderId, new ReservedAmount(username, amount));
      log.info(
          "Reserved {} from user {} balance for order {}. New balance: {}",
          amount,
          username,
          orderId,
          user.getBalance());
      return true;
    }

    log.warn(
        "Insufficient balance for user {}: required={}, available={}",
        username,
        amount,
        user.getBalance());
    return false;
  }

  /**
   * Add to user's balance (for SELL orders or refunds).
   *
   * @param username The username
   * @param amount The amount to add
   */
  @Transactional
  public void addBalance(String username, BigDecimal amount) {
    userRepository
        .findByUsername(username)
        .ifPresent(
            user -> {
              user.addBalance(amount);
              userRepository.save(user);
              log.info("Added {} to user {} balance. New balance: {}", amount, username, user.getBalance());
            });
  }

  /**
   * Refund balance for a rejected order.
   *
   * @param username The username
   * @param amount The amount to refund
   */
  @Transactional
  public void refundBalance(String username, BigDecimal amount) {
    addBalance(username, amount);
    log.info("Refunded {} to user {}", amount, username);
  }

  /**
   * Handle order completion - clears reservation tracking.
   *
   * <p>Called when an order is FILLED. The balance was already deducted, so just clear tracking.
   *
   * @param orderId The order ID
   */
  public void onOrderFilled(String orderId) {
    ReservedAmount removed = reservedAmounts.remove(orderId);
    if (removed != null) {
      log.info("Order {} filled. Cleared reservation of {} for user {}", orderId, removed.amount(), removed.username());
    }
  }

  /**
   * Handle order rejection - refunds the reserved balance.
   *
   * <p>Called when an order is REJECTED after balance was reserved.
   *
   * @param orderId The order ID
   */
  @Transactional
  public void onOrderRejected(String orderId) {
    ReservedAmount removed = reservedAmounts.remove(orderId);
    if (removed != null) {
      refundBalance(removed.username(), removed.amount());
      log.info(
          "Order {} rejected. Refunded {} to user {}", orderId, removed.amount(), removed.username());
    }
  }
}
