package com.trading.priceMonitor.service;

import com.trading.priceMonitor.dto.AuthResult;
import com.trading.priceMonitor.entity.UserEntity;
import com.trading.priceMonitor.security.JwtService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserService userService;
  private final JwtService jwtService;

  public AuthService(UserService userService, JwtService jwtService) {
    this.userService = userService;
    this.jwtService = jwtService;
  }

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
