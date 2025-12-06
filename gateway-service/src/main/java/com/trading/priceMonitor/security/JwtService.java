package com.trading.priceMonitor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token generation and validation.
 *
 * <p>Uses HMAC-SHA256 (HS256) for signing tokens. Tokens are used for:
 *
 * <ul>
 *   <li>REST API authentication (Authorization: Bearer header)
 *   <li>WebSocket authentication (token query parameter)
 * </ul>
 *
 * <p>Note: Secret key is hardcoded for this demo project.
 */
@Service
public class JwtService {

  // Must be at least 256 bits (32 characters) for HS256
  // Hardcoded for simplicity; in production, this will be stored in environment variables
  private static final String SECRET_KEY =
      "electricity-trading-secret-key-must-be-at-least-256-bits-long";

  private static final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
  }

  /**
   * Generates a JWT token for the given username.
   *
   * @param username The username to encode in the token
   * @return A signed JWT token valid for 24 hours
   */
  public String generateToken(String username) {
    return Jwts.builder()
        .subject(username)
        .signWith(getSigningKey())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Extracts the username from a JWT token.
   *
   * @param token The JWT token
   * @return The username stored in the token's subject claim
   */
  public String extractUsername(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    return claims.getSubject();
  }

  /**
   * Validates a JWT token.
   *
   * @param token The JWT token to validate
   * @return true if the token is valid and not expired, false otherwise
   */
  public boolean isTokenValid(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
