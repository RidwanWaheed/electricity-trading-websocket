package com.trading.order.repository;

import com.trading.order.entity.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Order entities.
 *
 * <p>Spring Data JPA automatically implements these methods based on method names. No SQL needed!
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

  /**
   * Find an order by its unique order ID.
   *
   * @param orderId The UUID assigned when the order was created
   * @return The order if found
   */
  Optional<OrderEntity> findByOrderId(String orderId);

  /**
   * Find all orders for a specific user.
   *
   * @param username The username of the trader
   * @return List of orders, most recent first
   */
  List<OrderEntity> findByUsernameOrderByCreatedAtDesc(String username);
}
