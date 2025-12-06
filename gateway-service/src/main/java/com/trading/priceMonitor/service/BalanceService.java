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
 * <p>Tracks:
 * <ul>
 *   <li>BUY orders: Reserved amounts that can be refunded if rejected
 *   <li>SELL orders: Pending credits to be added when filled
 * </ul>
 */
@Service
public class BalanceService {

  private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

  private final UserRepository userRepository;

  /**
   * Tracks reserved amounts for pending BUY orders.
   *
   * <p>Key: orderId, Value: PendingOrder (username + amount)
   */
  private final ConcurrentHashMap<String, PendingOrder> pendingBuyOrders = new ConcurrentHashMap<>();

  /**
   * Tracks pending SELL orders waiting for fill to credit balance.
   *
   * <p>Key: orderId, Value: PendingOrder (username + amount)
   */
  private final ConcurrentHashMap<String, PendingOrder> pendingSellOrders = new ConcurrentHashMap<>();

  public BalanceService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** Holds information about a pending order (BUY or SELL). */
  private record PendingOrder(String username, BigDecimal amount) {}

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
      pendingBuyOrders.put(orderId, new PendingOrder(username, amount));
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
    // Check if this was a BUY order - just clear reservation
    PendingOrder buyOrder = pendingBuyOrders.remove(orderId);
    if (buyOrder != null) {
      log.info(
          "BUY order {} filled. Cleared reservation of {} for user {}",
          orderId,
          buyOrder.amount(),
          buyOrder.username());
      return;
    }

    // Check if this was a SELL order - credit the balance
    PendingOrder sellOrder = pendingSellOrders.remove(orderId);
    if (sellOrder != null) {
      addBalance(sellOrder.username(), sellOrder.amount());
      log.info(
          "SELL order {} filled. Credited {} to user {}",
          orderId,
          sellOrder.amount(),
          sellOrder.username());
    }
  }

  /**
   * Handle order rejection - refunds the reserved balance for BUY orders.
   *
   * <p>Called when an order is REJECTED. For BUY orders, refunds the reserved amount. For SELL
   * orders, just clears tracking (no balance was deducted).
   *
   * @param orderId The order ID
   */
  @Transactional
  public void onOrderRejected(String orderId) {
    // Check if this was a BUY order - refund the reservation
    PendingOrder buyOrder = pendingBuyOrders.remove(orderId);
    if (buyOrder != null) {
      refundBalance(buyOrder.username(), buyOrder.amount());
      log.info(
          "BUY order {} rejected. Refunded {} to user {}",
          orderId,
          buyOrder.amount(),
          buyOrder.username());
      return;
    }

    // Check if this was a SELL order - just clear tracking (no refund needed)
    PendingOrder sellOrder = pendingSellOrders.remove(orderId);
    if (sellOrder != null) {
      log.info(
          "SELL order {} rejected. Cleared pending credit of {} for user {}",
          orderId,
          sellOrder.amount(),
          sellOrder.username());
    }
  }

  /**
   * Track a pending SELL order for later credit when filled.
   *
   * @param orderId The order ID
   * @param username The username
   * @param amount The amount to credit when filled (price * quantity)
   */
  public void trackSellOrder(String orderId, String username, BigDecimal amount) {
    pendingSellOrders.put(orderId, new PendingOrder(username, amount));
    log.info("Tracking SELL order {} for user {}. Will credit {} when filled", orderId, username, amount);
  }
}
