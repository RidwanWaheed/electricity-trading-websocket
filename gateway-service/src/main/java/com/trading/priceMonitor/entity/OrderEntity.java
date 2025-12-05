package com.trading.priceMonitor.entity;

import com.trading.priceMonitor.model.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false, unique = true, length = 50)
  private String orderId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Setter
  @Column(nullable = false, length = 20)
  private String region;

  @Setter
  @Column(name = "order_type", nullable = false, length = 10)
  private String orderType;

  @Setter
  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal quantity;

  @Setter
  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal price;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public OrderEntity(
      String orderId,
      UserEntity user,
      String region,
      String orderType,
      BigDecimal quantity,
      BigDecimal price) {
    this.orderId = orderId;
    this.user = user;
    this.region = region;
    this.orderType = orderType;
    this.quantity = quantity;
    this.price = price;
    this.status = Status.PENDING;
    this.createdAt = Instant.now();
  }
}
