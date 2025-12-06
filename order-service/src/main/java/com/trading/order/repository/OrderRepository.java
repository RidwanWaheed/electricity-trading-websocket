package com.trading.order.repository;

import com.trading.order.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for Order entities. */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

  Optional<OrderEntity> findByOrderId(String orderId);

  List<OrderEntity> findByUsernameOrderByCreatedAtDesc(String username);
}
