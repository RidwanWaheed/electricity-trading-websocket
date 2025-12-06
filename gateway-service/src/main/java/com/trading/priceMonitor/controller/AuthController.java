package com.trading.priceMonitor.controller;

import com.trading.priceMonitor.dto.AuthResponse;
import com.trading.priceMonitor.dto.AuthResult;
import com.trading.priceMonitor.dto.LoginRequest;
import com.trading.priceMonitor.dto.RegisterRequest;
import com.trading.priceMonitor.service.AuthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    AuthResult result = authService.authenticate(request.username(), request.password());

    if (result.success()) {
      return ResponseEntity.ok(
          new AuthResponse(result.token(), result.username(), result.balance()));
    } else {
      return ResponseEntity.status(401).body(Map.of("error", result.error()));
    }
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    AuthResult result = authService.register(request.username(), request.password());

    if (result.success()) {
      return ResponseEntity.ok(
          new AuthResponse(result.token(), result.username(), result.balance()));
    } else {
      return ResponseEntity.badRequest().body(Map.of("error", result.error()));
    }
  }
}
