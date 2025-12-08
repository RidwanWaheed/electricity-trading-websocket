package com.trading.order.entity;

import com.trading.common.OrderStatus;
import com.trading.common.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing an order in the database.
 *
 * <p>Tracks the full lifecycle of an order from PENDING through FILLED/REJECTED.
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String orderId;

  /** Correlation ID for distributed tracing across services */
  @Column(nullable = false)
  private String correlationId;

  @Column(nullable = false)
  private String username;

  /**
   * Trading region stored as String in database.
   *
   * <p>Using @Enumerated(EnumType.STRING) means the database stores "NORTH", "SOUTH", etc.
   * as actual strings, not ordinal numbers. This is safer because:
   * <ul>
   *   <li>Reordering enum constants doesn't break existing data
   *   <li>Database values are human-readable
   *   <li>Easier to debug and query directly in SQL
   * </ul>
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Region region;

  @Column(nullable = false)
  private String orderType;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal quantity;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  /** Assigned when M7 acknowledges the order */
  private String m7ReferenceId;

  /** May differ from requested price */
  @Column(precision = 19, scale = 4)
  private BigDecimal executionPrice;

  private String rejectReason;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  /** Required by JPA */
  protected OrderEntity() {}

  public OrderEntity(
      String orderId,
      String correlationId,
      String username,
      Region region,
      String orderType,
      BigDecimal quantity,
      BigDecimal price) {
    this.orderId = orderId;
    this.correlationId = correlationId;
    this.username = username;
    this.region = region;
    this.orderType = orderType;
    this.quantity = quantity;
    this.price = price;
    this.status = OrderStatus.PENDING;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getUsername() {
    return username;
  }

  public Region getRegion() {
    return region;
  }

  public String getOrderType() {
    return orderType;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public String getM7ReferenceId() {
    return m7ReferenceId;
  }

  public BigDecimal getExecutionPrice() {
    return executionPrice;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  // Status transition methods

  /**
   * Mark order as submitted (M7 acknowledged).
   *
   * @param m7ReferenceId The reference ID assigned by M7
   * @throws IllegalArgumentException if m7ReferenceId is null or blank
   * @throws IllegalStateException if order is not in PENDING state
   */
  public void markSubmitted(String m7ReferenceId) {
    if (m7ReferenceId == null || m7ReferenceId.isBlank()) {
      throw new IllegalArgumentException("M7 reference ID cannot be null or blank");
    }
    if (this.status != OrderStatus.PENDING) {
      throw new IllegalStateException(
          "Cannot mark as SUBMITTED: order is " + this.status + ", expected PENDING");
    }
    this.status = OrderStatus.SUBMITTED;
    this.m7ReferenceId = m7ReferenceId;
    this.updatedAt = Instant.now();
  }

  /**
   * Mark order as filled (executed by M7).
   *
   * @param executionPrice The price at which the order was filled
   * @throws IllegalArgumentException if executionPrice is null
   * @throws IllegalStateException if order is not in SUBMITTED state
   */
  public void markFilled(BigDecimal executionPrice) {
    if (executionPrice == null) {
      throw new IllegalArgumentException("Execution price cannot be null");
    }
    if (this.status != OrderStatus.SUBMITTED) {
      throw new IllegalStateException(
          "Cannot mark as FILLED: order is " + this.status + ", expected SUBMITTED");
    }
    this.status = OrderStatus.FILLED;
    this.executionPrice = executionPrice;
    this.updatedAt = Instant.now();
  }

  /**
   * Mark order as rejected by M7.
   *
   * @param reason The reason for rejection
   * @throws IllegalStateException if order is not in SUBMITTED state
   */
  public void markRejected(String reason) {
    if (this.status != OrderStatus.SUBMITTED) {
      throw new IllegalStateException(
          "Cannot mark as REJECTED: order is " + this.status + ", expected SUBMITTED");
    }
    this.status = OrderStatus.REJECTED;
    this.rejectReason = reason;
    this.updatedAt = Instant.now();
  }
}
