package com.trading.priceMonitor.repository;

import com.trading.priceMonitor.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for user accounts. */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByUsername(String username);

  boolean existsByUsername(String username);
}
