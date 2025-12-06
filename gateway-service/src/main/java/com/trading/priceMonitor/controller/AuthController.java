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

/**
 * REST controller for authentication endpoints.
 *
 * <p>Provides public endpoints for user login and registration. On successful authentication,
 * returns a JWT token that clients use for subsequent API calls and WebSocket connections.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>POST /api/auth/login - Authenticate existing user
 *   <li>POST /api/auth/register - Create new user account
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Authenticates a user and returns a JWT token.
   *
   * @param request Login credentials (username, password)
   * @return 200 OK with token on success, 401 Unauthorized on failure
   */
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

  /**
   * Registers a new user account and returns a JWT token.
   *
   * @param request Registration details (username, password)
   * @return 200 OK with token on success, 400 Bad Request if username taken
   */
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
