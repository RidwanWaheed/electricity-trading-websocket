package com.trading.priceMonitor.security;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket handshake interceptor for JWT authentication.
 *
 * <p>Extracts and validates JWT tokens during the WebSocket handshake phase. Unlike REST APIs where
 * tokens are sent in the Authorization header, WebSocket connections pass the token as a query
 * parameter (e.g., /ws-electricity?token=xxx).
 *
 * <p>Authentication flow:
 *
 * <ol>
 *   <li>Client initiates WebSocket connection with token in URL
 *   <li>This interceptor validates the token before the connection is established
 *   <li>If valid, username is stored in session attributes for later use
 *   <li>UserInterceptor then creates a Principal from the stored username
 * </ol>
 *
 * <p>Connections without a valid JWT token are rejected.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtService jwtService;

  public JwtHandshakeInterceptor(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  /**
   * Validates JWT token before WebSocket handshake completes.
   *
   * <p>If a valid token is provided, extracts the username and stores it in session attributes for
   * use by UserInterceptor.
   *
   * @param attributes Session attributes that persist for the WebSocket session
   * @return true to proceed with handshake, false to reject connection
   */
  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    if (request instanceof ServletServerHttpRequest servletRequest) {
      String token = servletRequest.getServletRequest().getParameter("token");

      if (token != null && jwtService.isTokenValid(token)) {
        String username = jwtService.extractUsername(token);
        attributes.put("username", username);
        return true;
      }
    }
    // Reject connections without valid JWT token
    return false;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // No post-handshake processing needed
  }
}
