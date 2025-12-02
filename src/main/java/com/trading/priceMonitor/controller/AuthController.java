package com.trading.priceMonitor.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trading.priceMonitor.security.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final JwtService jwtService;

  // Hardcoded users for demo (use database in production)
  private static final Map<String, String> USERS =
      Map.of(
          "trader1", "password1",
          "trader2", "password2",
          "admin", "admin123");

  public AuthController(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  public record LoginRequest(String username, String password) {}

  public record LoginResponse(String token, String username) {}

  @RequestMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    String storedPassword = USERS.get(request.username());

    if (storedPassword == null || !storedPassword.equals(request.password())) {
      return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
      String token = jwtService.generateToken(request.username());
      return ResponseEntity.ok(new LoginResponse(token, request.username()));
  }
}
