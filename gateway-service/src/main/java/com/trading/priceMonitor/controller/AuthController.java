package com.trading.priceMonitor.controller;

import com.trading.priceMonitor.dto.AuthResponse;
import com.trading.priceMonitor.dto.AuthResult;
import com.trading.priceMonitor.dto.LoginRequest;
import com.trading.priceMonitor.dto.RegisterRequest;
import com.trading.priceMonitor.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for authentication endpoints (login and registration). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "Login", description = "Authenticate with username and password to receive a JWT token")
  @SecurityRequirements
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

  @Operation(summary = "Register", description = "Create a new user account and receive a JWT token")
  @SecurityRequirements
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
