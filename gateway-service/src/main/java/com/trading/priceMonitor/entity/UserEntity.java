package com.trading.priceMonitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity for user accounts with balance management. */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

  /** Starting balance for new users (in EUR). */
  private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("10000.00");

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Setter
  @Column(nullable = false, unique = true, length = 50)
  private String username;

  @Setter
  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal balance;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public UserEntity(String username, String passwordHash) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.balance = DEFAULT_BALANCE;
    this.createdAt = Instant.now();
  }

  /**
   * Deduct amount from balance for a BUY order.
   *
   * @param amount The total cost (price * quantity)
   * @return true if sufficient balance, false otherwise
   */
  public boolean deductBalance(BigDecimal amount) {
    if (balance.compareTo(amount) < 0) {
      return false;
    }
    balance = balance.subtract(amount);
    return true;
  }

  /**
   * Add amount to balance for a SELL order or refund.
   *
   * @param amount The amount to add
   */
  public void addBalance(BigDecimal amount) {
    balance = balance.add(amount);
  }
}
