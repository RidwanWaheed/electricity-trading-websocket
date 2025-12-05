package com.trading.priceMonitor.service;

import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public Optional<UserEntity> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Transactional
  public UserEntity createUser(String username, String rawPassword) {
    if (existsByUsername(username)) {
      throw new IllegalArgumentException("Username already exists");
    }

    if (rawPassword == null || rawPassword.length() < 6) {
      throw new IllegalArgumentException("Password must be at least 6 characters");
    }

    String hashedPassword = passwordEncoder.encode(rawPassword);
    UserEntity user = new UserEntity(username, hashedPassword);
    return userRepository.save(user);
  }

  public boolean verifyPassword(String rawPassword, UserEntity user) {
    return passwordEncoder.matches(rawPassword, user.getPasswordHash());
  }
}
