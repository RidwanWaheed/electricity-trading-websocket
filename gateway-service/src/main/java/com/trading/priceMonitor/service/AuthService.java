package com.trading.priceMonitor.service;

import com.trading.priceMonitor.dto.AuthResult;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.security.JwtService;
import org.springframework.stereotype.Service;

/**
 * Service handling user authentication and registration.
 *
 * <p>Coordinates between UserService (credential verification) and JwtService (token generation) to
 * provide a clean authentication API for the controller layer.
 *
 * <p>Returns AuthResult which encapsulates success/failure states, avoiding exceptions for normal
 * authentication failures (e.g., wrong password).
 */
@Service
public class AuthService {

  private final UserService userService;
  private final JwtService jwtService;

  public AuthService(UserService userService, JwtService jwtService) {
    this.userService = userService;
    this.jwtService = jwtService;
  }

  /**
   * Authenticates a user with username and password.
   *
   * @param username The username
   * @param password The plaintext password
   * @return AuthResult with JWT token on success, or error message on failure
   */
  public AuthResult authenticate(String username, String password) {
    var userOptional = userService.findByUsername(username);

    if (userOptional.isEmpty()) {
      return AuthResult.failure("Invalid credentials");
    }

    UserEntity user = userOptional.get();

    if (!userService.verifyPassword(password, user)) {
      return AuthResult.failure("Invalid credentials");
    }

    String token = jwtService.generateToken(user.getUsername());
    return AuthResult.success(token, user.getUsername(), user.getBalance().toString());
  }

  /**
   * Registers a new user account.
   *
   * @param username The desired username
   * @param password The plaintext password (will be hashed)
   * @return AuthResult with JWT token on success, or error message if username taken
   */
  public AuthResult register(String username, String password) {
    try {
      UserEntity user = userService.createUser(username, password);
      String token = jwtService.generateToken(user.getUsername());
      return AuthResult.success(token, user.getUsername(), user.getBalance().toString());
    } catch (IllegalArgumentException e) {
      return AuthResult.failure(e.getMessage());
    }
  }
}
