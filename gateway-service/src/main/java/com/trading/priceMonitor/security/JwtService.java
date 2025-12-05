package com.trading.priceMonitor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  // Must be at least 256 bits (32 characters) for HS256
  // Hardcoded for simplicity and since this a demo project; in production, use a secure method to
  // manage secrets
  private static final String SECRET_KEY =
      "electricity-trading-secret-key-must-be-at-least-256-bits-long";
  private static final long EXPIRATION_TIME = 86400000; // 24 hours in milliseconds

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
  }

  public String generateToken(String username) {
    return Jwts.builder()
        .subject(username)
        .signWith(getSigningKey())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
        .signWith(getSigningKey())
        .compact();
  }

  public String extractUsername(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    return claims.getSubject();
  }

  public boolean isTokenValid(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
