package com.trading.priceMonitor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.trading.priceMonitor.entity.OrderEntity;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.model.Status;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByOrderId(String orderId);

    List<OrderEntity> findByUserOrderByCreatedAtDesc(UserEntity user);

    List<OrderEntity> findByUserAndStatus(UserEntity user, Status status);

    boolean existsByOrderId(String orderId);

    long countByStatus(Status status);
}
